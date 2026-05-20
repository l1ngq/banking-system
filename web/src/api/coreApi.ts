import type { Account, CurrencyCode, Transaction, UserProfile } from '../types/banking';
import { API_URLS, DEMO_AUTH, type DemoUser } from './config';
import { refreshCsrf, request } from './http';

type BackendCurrency = 'USD' | 'EUR' | 'RUB';
type BackendAccountType = 'CHECKING' | 'SAVINGS';
type BackendAccountStatus = 'ACTIVE' | 'CLOSED';
type BackendTransactionType = 'TRANSFER' | 'INTEREST' | 'DEPOSIT' | 'WITHDRAWAL';
type BackendTransactionStatus = 'COMPLETED' | 'FAILED';

type AuthState = {
  authenticated: boolean;
  email: string | null;
  role: string | null;
};

type BackendAccount = {
  id: number;
  userId?: string;
  currency: BackendCurrency;
  balance: number | string;
  type: BackendAccountType;
  status: BackendAccountStatus;
  lastInterestAccruedDate?: string | null;
  createdAt?: string;
};

type AccountList = {
  accounts: BackendAccount[];
  total: number;
};

export type BackendTransaction = {
  id: string;
  fromAccountId: number | null;
  toAccountId: number | null;
  amount: number | string;
  convertedAmount?: number | string | null;
  currency: BackendCurrency;
  type: BackendTransactionType;
  status: BackendTransactionStatus;
  createdAt: string;
};

export type TransferPayload = {
  fromAccountId: string;
  toAccountId: string;
  amount: number;
  currency: CurrencyCode;
};

const SUPPORTED_CURRENCIES: BackendCurrency[] = ['USD', 'EUR', 'RUB'];

function readCookie(name: string) {
  return document.cookie
    .split('; ')
    .find((row) => row.startsWith(`${name}=`))
    ?.split('=')[1];
}

function csrfTokenHeader(): Record<string, string> {
  const token = readCookie('XSRF-TOKEN');
  return token ? { 'X-XSRF-TOKEN': decodeURIComponent(token) } : {};
}

async function postFormLogin(url: string, body: URLSearchParams) {
  const response = await fetch(url, {
    method: 'POST',
    credentials: 'include',
    redirect: 'manual',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      ...csrfTokenHeader(),
    },
    body,
  });

  if (![0, 200, 204, 302, 303].includes(response.status)) {
    throw new Error(`Login failed: ${response.status}`);
  }
}

function toBackendCurrency(currency: CurrencyCode): BackendCurrency {
  if (SUPPORTED_CURRENCIES.includes(currency as BackendCurrency)) {
    return currency as BackendCurrency;
  }
  throw new Error('Доступны только валюты USD, EUR и RUB');
}

function toBackendAccountType(type: Account['type']): BackendAccountType {
  if (type === 'saving') return 'SAVINGS';
  if (type === 'debit') return 'CHECKING';
  throw new Error('Кредитный счёт сейчас недоступен');
}

function fromBackendAccountType(type: BackendAccountType): Account['type'] {
  return type === 'SAVINGS' ? 'saving' : 'debit';
}

export function makeDisplayAccountNumber(id: string | number) {
  const safeId = String(id).replace(/\D/g, '') || String(id);
  return `40817${safeId.padStart(15, '0').slice(-15)}`;
}

export function mapAccount(account: BackendAccount): Account {
  const type = fromBackendAccountType(account.type);
  const currency = account.currency as CurrencyCode;

  return {
    id: String(account.id),
    name: `${currency} ${type === 'saving' ? 'накопительный' : 'текущий'} счёт`,
    number: makeDisplayAccountNumber(account.id),
    balance: Number(account.balance ?? 0),
    currency,
    type,
    status: account.status === 'ACTIVE' ? 'active' : 'closed',
    interestRate: type === 'saving' ? 12.5 : undefined,
    openedAt: account.createdAt ?? new Date().toISOString(),
  };
}

export function mapTransaction(transaction: BackendTransaction, account: Account): Transaction {
  const isOutgoing = String(transaction.fromAccountId ?? '') === account.id;
  const isIncoming = String(transaction.toAccountId ?? '') === account.id;
  const type: Transaction['type'] =
    transaction.type === 'DEPOSIT'
      ? 'income'
      : transaction.type === 'TRANSFER'
        ? 'transfer'
        : 'outcome';

  const displayedAmount = Number(
    transaction.type === 'TRANSFER' && isIncoming
      ? transaction.convertedAmount ?? transaction.amount ?? 0
      : transaction.amount ?? 0,
  );
  const sign = type === 'income' || (transaction.type === 'TRANSFER' && isIncoming) ? 1 : -1;
  const amount = displayedAmount * sign;

  return {
    id: `${transaction.id}-${account.id}`,
    title:
      transaction.type === 'TRANSFER'
        ? isOutgoing
          ? `Перевод на счёт ${transaction.toAccountId}`
          : `Перевод со счёта ${transaction.fromAccountId}`
        : transaction.type === 'DEPOSIT'
          ? 'Пополнение счёта'
          : transaction.type === 'INTEREST'
            ? 'Начисление процентов'
            : 'Оплата или списание',
    category:
      transaction.type === 'TRANSFER'
        ? 'Переводы'
        : transaction.type === 'DEPOSIT'
          ? 'Пополнения'
          : transaction.type === 'INTEREST'
            ? 'Накопления'
            : 'Платежи',
    amount,
    currency: account.currency,
    createdAt: transaction.createdAt,
    status: transaction.status === 'COMPLETED' ? 'success' : 'failed',
    accountId: account.id,
    type,
  };
}

export const coreApi = {
  async getAuthState() {
    return request<AuthState>(`${API_URLS.core}/api/auth/me`, { raw: false });
  },

  async register(email: string, password: string) {
    await refreshCsrf(API_URLS.core);
    return request<unknown>(`${API_URLS.core}/api/auth/registration`, {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  },

  async login(email: string, password: string) {
    await refreshCsrf(API_URLS.core);
    const body = new URLSearchParams();
    body.set('email', email);
    body.set('password', password);

    await postFormLogin(`${API_URLS.core}/api/auth/login`, body);
  },

  async logout() {
    await refreshCsrf(API_URLS.core);
    const response = await fetch(`${API_URLS.core}/api/auth/logout`, {
      method: 'POST',
      credentials: 'include',
      redirect: 'manual',
      headers: csrfTokenHeader(),
    });

    if (![0, 200, 204, 302, 303].includes(response.status)) {
      throw new Error(`Logout failed: ${response.status}`);
    }
  },

  async requireSession() {
    await refreshCsrf(API_URLS.core);
    const me = await this.getAuthState();
    if (!me.authenticated) {
      throw new Error('Войдите в личный кабинет');
    }
    return me;
  },

  async signInDemoSession(user: DemoUser = DEMO_AUTH) {
    if (!DEMO_AUTH.enabled) {
      throw new Error('Демо-вход выключен в .env: VITE_AUTO_LOGIN=false');
    }

    try {
      await this.register(user.email, user.password);
    } catch {
      // Пользователь мог быть создан раньше; ниже выполняется обычный вход.
    }

    await this.login(user.email, user.password);
    return this.getAuthState();
  },

  async getProfile(): Promise<Partial<UserProfile>> {
    const auth = await this.requireSession();
    return {
      email: auth.email ?? DEMO_AUTH.email,
      role: auth.role === 'ADMIN' ? 'admin' : 'user',
    };
  },

  async getAccounts(): Promise<Account[]> {
    await this.requireSession();
    const list = await request<AccountList>(`${API_URLS.core}/api/accounts/my`);
    return (list.accounts ?? []).map(mapAccount);
  },

  async createAccount(payload: Pick<Account, 'currency' | 'type'>): Promise<Account> {
    await this.requireSession();
    const account = await request<BackendAccount>(`${API_URLS.core}/api/accounts`, {
      method: 'POST',
      body: JSON.stringify({
        currency: toBackendCurrency(payload.currency),
        type: toBackendAccountType(payload.type),
      }),
    });
    return mapAccount(account);
  },

  async deposit(accountId: string, amount: number): Promise<Account> {
    await this.requireSession();
    const account = await request<BackendAccount>(`${API_URLS.core}/api/accounts/${accountId}/deposit`, {
      method: 'POST',
      body: JSON.stringify({ amount }),
    });
    return mapAccount(account);
  },

  async withdraw(accountId: string, amount: number): Promise<Account> {
    await this.requireSession();
    const account = await request<BackendAccount>(`${API_URLS.core}/api/accounts/${accountId}/withdraw`, {
      method: 'POST',
      body: JSON.stringify({ amount }),
    });
    return mapAccount(account);
  },

  async closeAccount(accountId: string) {
    await this.requireSession();
    return request<unknown>(`${API_URLS.core}/api/accounts/${accountId}`, {
      method: 'DELETE',
    });
  },

  async transfer(payload: TransferPayload): Promise<BackendTransaction> {
    await this.requireSession();
    return request<BackendTransaction>(`${API_URLS.core}/api/transfers`, {
      method: 'POST',
      body: JSON.stringify({
        fromAccountId: Number(payload.fromAccountId),
        toAccountId: Number(payload.toAccountId),
        amount: payload.amount,
        currency: toBackendCurrency(payload.currency),
      }),
    });
  },

  async getHistory(account: Account): Promise<Transaction[]> {
    await this.requireSession();
    const history = await request<BackendTransaction[]>(
      `${API_URLS.core}/api/transfers/history?accountId=${encodeURIComponent(account.id)}`,
    );
    return (history ?? []).map((item) => mapTransaction(item, account));
  },
};
