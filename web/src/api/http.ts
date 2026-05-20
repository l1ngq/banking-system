type RequestOptions = RequestInit & {
  timeoutMs?: number;
  raw?: boolean;
};

type UniversalResponse<T> = {
  code?: number;
  message?: string;
  data?: T;
};

function readCookie(name: string) {
  return document.cookie
    .split('; ')
    .find((row) => row.startsWith(`${name}=`))
    ?.split('=')[1];
}

function csrfHeader() {
  const token = readCookie('XSRF-TOKEN');
  return token ? { 'X-XSRF-TOKEN': decodeURIComponent(token) } : {};
}

function isFormDataBody(body: BodyInit | null | undefined) {
  return typeof URLSearchParams !== 'undefined' && body instanceof URLSearchParams;
}

function needsCsrf(method?: string) {
  const normalized = (method ?? 'GET').toUpperCase();
  return !['GET', 'HEAD', 'OPTIONS'].includes(normalized);
}

function toFriendlyError(message: string, status?: number) {
  const normalized = message.toLowerCase();

  if (status === 401 || normalized.includes('unauthorized')) {
    return 'Войдите в личный кабинет';
  }

  if (normalized.includes('insufficient funds') || normalized.includes('недостаточно средств')) {
    return 'Недостаточно средств на выбранном счёте';
  }

  if (normalized.includes('amount must be greater than zero')) {
    return 'Сумма должна быть больше нуля';
  }

  if (normalized.includes('account not found') || normalized.includes('счёт не найден')) {
    return 'Счёт не найден';
  }

  if (normalized.includes('does not belong') || normalized.includes('не принадлежит')) {
    return 'Этот счёт недоступен текущему пользователю';
  }

  if (normalized.includes('currency') || normalized.includes('валюта')) {
    return message.replace('Currency service unavailable', 'Сервис валют временно недоступен');
  }

  return message;
}

export async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), options.timeoutMs ?? 8000);
  const headers = new Headers(options.headers);
  const method = options.method ?? 'GET';

  if (options.body && !isFormDataBody(options.body as BodyInit) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  if (needsCsrf(method)) {
    Object.entries(csrfHeader()).forEach(([key, value]) => headers.set(key, value));
  }

  try {
    let response: Response;

    try {
      response = await fetch(url, {
        ...options,
        method,
        credentials: 'include',
        headers,
        signal: controller.signal,
      });
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        throw new Error('Сервер не ответил вовремя. Проверьте, что серверная часть запущена.', { cause: error });
      }

      throw new Error('Не удалось подключиться к серверу. Проверьте запуск серверной части.', { cause: error });
    }

    if (!response.ok) {
      let message = `Request failed: ${response.status}`;
      try {
        const payload = (await response.json()) as UniversalResponse<unknown>;
        message = payload.message || message;
      } catch {
        // response is not JSON
      }
      throw new Error(toFriendlyError(message, response.status));
    }

    if (response.status === 204) {
      return undefined as T;
    }

    const payload = (await response.json()) as UniversalResponse<T> | T;

    if (options.raw) {
      return payload as T;
    }

    if (payload && typeof payload === 'object' && 'data' in payload) {
      const wrapped = payload as UniversalResponse<T>;
      if (typeof wrapped.code === 'number' && wrapped.code !== 0) {
        throw new Error(toFriendlyError(wrapped.message || 'Сервер вернул ошибку'));
      }
      return wrapped.data as T;
    }

    return payload as T;
  } finally {
    window.clearTimeout(timeout);
  }
}

export async function withFallback<T>(loader: () => Promise<T>, fallback: T): Promise<T> {
  try {
    return await loader();
  } catch {
    return fallback;
  }
}

export async function refreshCsrf(coreUrl: string) {
  try {
    await request(`${coreUrl}/api/auth/me`, { raw: true, timeoutMs: 5000 });
  } catch {
    // /me can return 401 before login; the CSRF cookie is still issued by backend.
  }
}
