import { useEffect, useMemo, useState } from 'react';

import { getDemoUserByEmail, getDisplayNameByEmail, type DemoUser } from '../api/config';
import { coreApi, makeDisplayAccountNumber } from '../api/coreApi';
import { currencyApi } from '../api/currencyApi';
import { createBankingStateForEmail } from '../data/mockData';
import type {
  Action,
  Account,
  AssistantMessage,
  BankingState,
  CurrencyCode,
  Theme,
  Transaction,
} from '../types/banking';
import { getTotalBalance } from '../utils/calculations';

const STORAGE_KEY = 'mik-bank-state-coursework-final-v1';
const DEMO_RUB_BALANCE = 150_000;
const DEMO_USD_BALANCE = 1_000;
const DEMO_EUR_BALANCE = 1_000;

type ToastType = 'success' | 'info' | 'error';
type AuthStatus = 'checking' | 'guest' | 'authenticated';

export interface AuthViewState {
  status: AuthStatus;
  email: string | null;
  error: string | null;
}

export interface Toast {
  id: string;
  type: ToastType;
  title: string;
  text?: string;
}

function storageKeyForEmail(email?: string | null) {
  const normalizedEmail = email?.trim().toLowerCase();
  return normalizedEmail ? `${STORAGE_KEY}:${normalizedEmail}` : STORAGE_KEY;
}

function readInitialState(email?: string | null): BankingState {
  const fallbackState = createBankingStateForEmail(email);
  const demoUser = getDemoUserByEmail(email);

  try {
    const saved = localStorage.getItem(storageKeyForEmail(email));
    if (!saved) return fallbackState;

    const parsed = JSON.parse(saved) as BankingState;
    const savedTheme = parsed.profile?.theme ?? fallbackState.profile.theme;

    // Для демо-пользователей персональные витрины берём из актуального seed-файла.
    // Так старые данные из localStorage не смогут вернуть неправильный город, цели или расходы.
    if (demoUser) {
      return {
        ...fallbackState,
        profile: {
          ...fallbackState.profile,
          theme: savedTheme,
        },
        rates: parsed.rates?.filter((rate) => rate.code === 'USD' || rate.code === 'EUR').length
          ? parsed.rates.filter((rate) => rate.code === 'USD' || rate.code === 'EUR')
          : fallbackState.rates,
        news: parsed.news?.length ? parsed.news : fallbackState.news,
        accounts: (parsed.accounts ?? fallbackState.accounts).filter((account) => isSupportedBackendCurrency(account.currency)),
      };
    }

    return {
      ...fallbackState,
      ...parsed,
      profile: {
        ...fallbackState.profile,
        ...parsed.profile,
      },
      rates: parsed.rates?.filter((rate) => rate.code === 'USD' || rate.code === 'EUR').length
        ? parsed.rates.filter((rate) => rate.code === 'USD' || rate.code === 'EUR')
        : fallbackState.rates,
      news: parsed.news?.length ? parsed.news : fallbackState.news,
      accounts: (parsed.accounts ?? fallbackState.accounts).filter((account) => isSupportedBackendCurrency(account.currency)),
      cards: (parsed.cards ?? fallbackState.cards).filter((card) => isSupportedBackendCurrency(card.currency)),
      transactions: (parsed.transactions ?? fallbackState.transactions).filter((transaction) => isSupportedBackendCurrency(transaction.currency)),
      assistantMessages: parsed.assistantMessages ?? fallbackState.assistantMessages,
      goals: (parsed.goals ?? fallbackState.goals).filter((goal) => isSupportedBackendCurrency(goal.currency)),
      cashbackCategories: parsed.cashbackCategories ?? fallbackState.cashbackCategories,
    };
  } catch {
    return fallbackState;
  }
}

function uniqueTransactions(items: Transaction[]) {
  const map = new Map<string, Transaction>();
  items.forEach((item) => map.set(item.id, item));
  return Array.from(map.values()).sort(
    (left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime(),
  );
}

function replaceAccount(accounts: Account[], updated: Account) {
  const exists = accounts.some((account) => account.id === updated.id);
  if (!exists) return [updated, ...accounts];
  return accounts.map((account) => (account.id === updated.id ? { ...account, ...updated } : account));
}

function resolveBackendAccountId(value: string, accounts: Account[]) {
  const normalized = value.trim();
  const byId = accounts.find((account) => account.id === normalized);
  if (byId) return byId.id;

  const byNumber = accounts.find((account) => account.number === normalized);
  if (byNumber) return byNumber.id;

  const digits = normalized.replace(/\D/g, '');
  const byGeneratedNumber = accounts.find((account) => account.number.replace(/\D/g, '') === digits);
  if (byGeneratedNumber) return byGeneratedNumber.id;

  const compactId = digits.replace(/^0+/, '');
  const byShortId = accounts.find((account) => account.id === compactId);
  if (byShortId) return byShortId.id;

  return compactId || normalized;
}

function isSupportedBackendCurrency(currency: CurrencyCode) {
  return currency === 'RUB' || currency === 'USD' || currency === 'EUR';
}

function isNumericAccountId(value: string) {
  return /^\d+$/.test(value.trim());
}

function isValidExternalAccountNumber(value: string) {
  return /^\d{10,30}$/.test(value.replace(/\D/g, ''));
}

function demoTargetBalance(currency: CurrencyCode) {
  if (currency === 'RUB') return DEMO_RUB_BALANCE;
  if (currency === 'USD') return DEMO_USD_BALANCE;
  if (currency === 'EUR') return DEMO_EUR_BALANCE;
  return 0;
}

function shouldPrepareDemoCabinet(email: string | null) {
  return Boolean(getDemoUserByEmail(email));
}

function personalizeAccountName(account: Account, email?: string | null): Account {
  const demoUser = getDemoUserByEmail(email);
  if (!demoUser) return account;

  const ownerSuffix = ` · ${demoUser.name}`;
  const nameByCurrency: Record<string, string> = {
    RUB: account.type === 'saving' ? 'Накопительный счёт' : 'Основной счёт',
    USD: 'USD счёт',
    EUR: 'EUR счёт',
  };

  return {
    ...account,
    name: `${nameByCurrency[account.currency] ?? account.name}${ownerSuffix}`,
  };
}

export function useBankingState() {
  const [state, setState] = useState<BankingState>(readInitialState);
  const [auth, setAuth] = useState<AuthViewState>({ status: 'checking', email: null, error: null });
  const [toasts, setToasts] = useState<Toast[]>([]);

  useEffect(() => {
    if (auth.status === 'authenticated' && auth.email) {
      localStorage.setItem(storageKeyForEmail(auth.email), JSON.stringify(state));
    }

    document.documentElement.dataset.theme = state.profile.theme;
  }, [auth.email, auth.status, state]);

  function notify(type: ToastType, title: string, text?: string) {
    const toast: Toast = { id: crypto.randomUUID(), type, title, text };
    setToasts((current) => [...current, toast]);

    window.setTimeout(() => {
      setToasts((current) => current.filter((item) => item.id !== toast.id));
    }, 3600);
  }

  function removeToast(id: string) {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }

  function updateAccountInState(account: Account) {
    setState((current) => ({
      ...current,
      accounts: replaceAccount(current.accounts, account),
    }));
  }

  async function refreshAccounts(emailOverride?: string | null) {
    const ownerEmail = emailOverride ?? auth.email;
    const accounts = (await coreApi.getAccounts()).map((account) => personalizeAccountName(account, ownerEmail));
    setState((current) => ({ ...current, accounts }));
    return accounts;
  }

  async function refreshHistory(accounts: Account[]) {
    const histories = await Promise.all(
      accounts
        .filter((account) => account.status === 'active')
        .map((account) => coreApi.getHistory(account).catch(() => [] as Transaction[])),
    );
    const backendTransactions = histories.flat();

    setState((current) => ({
      ...current,
      transactions: uniqueTransactions([...backendTransactions, ...current.transactions]),
    }));
  }

  async function refreshRates() {
    try {
      const rates = await currencyApi.getRates();
      setState((current) => ({ ...current, rates }));
      return rates;
    } catch {
      await currencyApi.seedDefaultRates();
      const rates = await currencyApi.getRates();
      setState((current) => ({ ...current, rates }));
      return rates;
    }
  }

  async function loadBackendData(showSuccessToast = false, emailOverride?: string | null) {
    const profile = await coreApi.getProfile();
    const profileEmail = profile.email ?? emailOverride ?? auth.email;
    const demoProfile = getDemoUserByEmail(profileEmail)
      ? createBankingStateForEmail(profileEmail).profile
      : null;

    setState((current) => ({
      ...current,
      profile: demoProfile
        ? {
            ...demoProfile,
            theme: current.profile.theme,
          }
        : {
            ...current.profile,
            ...profile,
            email: profileEmail ?? current.profile.email,
            fullName: getDisplayNameByEmail(profileEmail, current.profile.fullName),
          },
    }));

    await refreshRates();
    const accounts = await refreshAccounts(profileEmail);
    await refreshHistory(accounts);

    if (showSuccessToast) {
      notify('success', 'Данные обновлены');
    }
  }

  async function ensureDemoCabinet(emailOverride?: string | null) {
    const ownerEmail = emailOverride ?? auth.email;
    await refreshRates();

    let accounts = await refreshAccounts(ownerEmail);

    const requiredAccounts: Array<Pick<Account, 'currency' | 'type'>> = [
      { currency: 'RUB', type: 'debit' },
      { currency: 'USD', type: 'debit' },
      { currency: 'EUR', type: 'debit' },
    ];

    for (const required of requiredAccounts) {
      let account = accounts.find(
        (item) => item.status === 'active' && item.currency === required.currency && item.type === required.type,
      );

      if (!account) {
        account = personalizeAccountName(await coreApi.createAccount(required), ownerEmail);
        accounts = replaceAccount(accounts, account);
      }
    }

    for (const account of accounts.filter((item) => item.status === 'active' && isSupportedBackendCurrency(item.currency))) {
      const targetBalance = demoTargetBalance(account.currency);
      if (targetBalance > 0 && account.balance < targetBalance) {
        const updatedAccount = personalizeAccountName(
          await coreApi.deposit(account.id, Number((targetBalance - account.balance).toFixed(2))),
          ownerEmail,
        );
        accounts = replaceAccount(accounts, updatedAccount);
      }
    }

    setState((current) => ({ ...current, accounts }));
    await refreshHistory(accounts);
  }

  async function prepareDemoAccountIfNeeded(account: Account, amount: number) {
    if (!shouldPrepareDemoCabinet(auth.email) || account.balance >= amount) {
      return account;
    }

    const minimumBalance = Math.max(amount + demoTargetBalance(account.currency) * 0.1, amount);
    const refillAmount = Number((minimumBalance - account.balance).toFixed(2));
    const updatedAccount = personalizeAccountName(await coreApi.deposit(account.id, refillAmount), auth.email);
    updateAccountInState(updatedAccount);
    return updatedAccount;
  }

  async function bootstrapFromBackend() {
    setAuth({ status: 'checking', email: null, error: null });

    try {
      const session = await coreApi.getAuthState();
      if (!session.authenticated) {
        setAuth({ status: 'guest', email: null, error: null });
        return;
      }

      const sessionEmail = session.email;
      setState(readInitialState(sessionEmail));
      setAuth({ status: 'authenticated', email: sessionEmail, error: null });
      await loadBackendData(true, sessionEmail);
      if (shouldPrepareDemoCabinet(sessionEmail)) {
        await ensureDemoCabinet(sessionEmail);
      }
    } catch {
      setAuth({ status: 'guest', email: null, error: null });
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void bootstrapFromBackend();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function login(email: string, password: string) {
    setAuth({ status: 'checking', email, error: null });

    try {
      await coreApi.login(email, password);
      const session = await coreApi.getAuthState();
      const sessionEmail = session.email ?? email;
      setState(readInitialState(sessionEmail));
      setAuth({ status: 'authenticated', email: sessionEmail, error: null });
      await loadBackendData(true, sessionEmail);
      if (shouldPrepareDemoCabinet(sessionEmail)) {
        await ensureDemoCabinet(sessionEmail);
      }
      notify('success', 'Вы вошли в личный кабинет');
    } catch (error) {
      setAuth({
        status: 'guest',
        email: null,
        error: error instanceof Error ? error.message : 'Не удалось войти. Проверьте почту и пароль.',
      });
    }
  }

  async function register(email: string, password: string) {
    setAuth({ status: 'checking', email, error: null });

    try {
      await coreApi.register(email, password);
      await coreApi.login(email, password);
      const session = await coreApi.getAuthState();
      const sessionEmail = session.email ?? email;
      setState(readInitialState(sessionEmail));
      setAuth({ status: 'authenticated', email: sessionEmail, error: null });
      await loadBackendData(true, sessionEmail);
      if (shouldPrepareDemoCabinet(sessionEmail)) {
        await ensureDemoCabinet(sessionEmail);
      }
      notify('success', 'Аккаунт создан');
    } catch (error) {
      setAuth({
        status: 'guest',
        email: null,
        error: error instanceof Error ? error.message : 'Не удалось создать аккаунт.',
      });
    }
  }

  async function loginDemo(user: DemoUser) {
    setAuth({ status: 'checking', email: null, error: null });

    try {
      const session = await coreApi.signInDemoSession(user);
      const sessionEmail = session.email ?? user.email;
      setState(readInitialState(sessionEmail));
      setAuth({ status: 'authenticated', email: sessionEmail, error: null });
      await loadBackendData(false, sessionEmail);
      await ensureDemoCabinet(sessionEmail);
      notify('success', `Демо-кабинет ${user.name} готов`, 'Счета, баланс и курсы подготовлены');
    } catch (error) {
      setAuth({
        status: 'guest',
        email: null,
        error: error instanceof Error ? error.message : 'Не удалось войти в демо-аккаунт.',
      });
    }
  }

  async function logout() {
    try {
      await coreApi.logout();
    } catch {
      // The local session should be cleared in the UI even if the backend already ended it.
    }

    setAuth({ status: 'guest', email: null, error: null });
    setState(createBankingStateForEmail(null));
    notify('info', 'Вы вышли из личного кабинета');
  }

  function resetDemoData() {
    const email = auth.email;
    localStorage.removeItem(storageKeyForEmail(email));
    setState(createBankingStateForEmail(email));
    if (auth.status === 'authenticated') {
      void loadBackendData(false, email);
    }
    notify('info', 'Личные данные интерфейса обновлены');
  }

  function setTheme(theme: Theme) {
    setState((current) => ({
      ...current,
      profile: {
        ...current.profile,
        theme,
      },
    }));
    notify('success', theme === 'dark' ? 'Тёмная тема включена' : 'Светлая тема включена');
  }

  function updateProfile(payload: Partial<BankingState['profile']>) {
    setState((current) => ({
      ...current,
      profile: {
        ...current.profile,
        ...payload,
      },
    }));
    notify('info', 'Профиль обновлён');
  }

  async function openAccount(payload: { name: string; currency: CurrencyCode; type: Account['type'] }) {
    if (!isSupportedBackendCurrency(payload.currency)) {
      notify('error', 'Эта валюта сейчас недоступна');
      return;
    }

    try {
      const account = await coreApi.createAccount({ currency: payload.currency, type: payload.type });
      const namedAccount = payload.name
        ? { ...account, name: payload.name }
        : personalizeAccountName(account, auth.email);

      updateAccountInState(namedAccount);
      notify('success', 'Счёт открыт', namedAccount.name);
    } catch (error) {
      notify('error', 'Не удалось открыть счёт', error instanceof Error ? error.message : undefined);
    }
  }

  async function closeAccount(accountId: string) {
    const accounts = await refreshAccounts().catch(() => state.accounts);
    const account = accounts.find((item) => item.id === accountId);
    if (account && account.balance > 0) {
      notify('error', 'Нельзя закрыть счёт с остатком', 'Сначала переведите или снимите деньги');
      return;
    }

    try {
      await coreApi.closeAccount(accountId);
      await refreshAccounts();
      notify('info', 'Счёт закрыт');
    } catch (error) {
      notify('error', 'Не удалось закрыть счёт', error instanceof Error ? error.message : undefined);
    }
  }

  async function topUp(accountId: string, amount: number) {
    if (!amount || amount <= 0) {
      notify('error', 'Введите корректную сумму');
      return;
    }

    try {
      const accounts = await refreshAccounts().catch(() => state.accounts);
      const account = accounts.find((item) => item.id === accountId);
      if (!account) {
        notify('error', 'Счёт не найден');
        return;
      }

      const updatedAccount = personalizeAccountName(await coreApi.deposit(account.id, amount), auth.email);
      updateAccountInState(updatedAccount);

      const freshAccounts = await refreshAccounts();
      await refreshHistory(freshAccounts);
      notify('success', 'Счёт пополнен');
    } catch (error) {
      notify('error', 'Пополнение не выполнено', error instanceof Error ? error.message : undefined);
    }
  }

  async function transfer(payload: { fromAccountId: string; toAccountNumber: string; amount: number; transferMode?: string }) {
    if (!payload.amount || payload.amount <= 0 || payload.toAccountNumber.trim().length < 1) {
      notify('error', 'Проверьте данные перевода');
      return;
    }

    try {
      await refreshRates();
      const accounts = await refreshAccounts();
      let account = accounts.find((item) => item.id === payload.fromAccountId);
      if (!account) {
        notify('error', 'Счёт отправителя не найден');
        return;
      }

      if (payload.transferMode === 'external') {
        const recipientNumber = payload.toAccountNumber.replace(/\D/g, '');
        if (!isValidExternalAccountNumber(recipientNumber)) {
          notify('error', 'Введите корректный номер счёта получателя');
          return;
        }

        const draftTransaction: Transaction = {
          id: `external-draft-${crypto.randomUUID()}`,
          title: `Черновик перевода на счёт •${recipientNumber.slice(-4)}`,
          category: 'Перевод по номеру счёта',
          amount: -payload.amount,
          currency: account.currency,
          createdAt: new Date().toISOString(),
          status: 'pending',
          accountId: account.id,
          type: 'transfer',
        };

        setState((current) => ({
          ...current,
          transactions: uniqueTransactions([draftTransaction, ...current.transactions]),
        }));
        notify('info', 'Черновик перевода создан', 'Фактическая отправка будет доступна после реализации этой функции в системе банка');
        return;
      }

      account = await prepareDemoAccountIfNeeded(account, payload.amount);
      if (account.balance < payload.amount) {
        notify('error', 'Недостаточно средств', 'Сначала пополните выбранный счёт');
        return;
      }

      const toAccountId = resolveBackendAccountId(payload.toAccountNumber, accounts);
      if (!isNumericAccountId(toAccountId)) {
        notify('error', 'Счёт получателя не найден');
        return;
      }

      await coreApi.transfer({
        fromAccountId: account.id,
        toAccountId,
        amount: payload.amount,
        currency: account.currency,
      });

      const freshAccounts = await refreshAccounts();
      await refreshHistory(freshAccounts);
      notify('success', 'Перевод отправлен', `Получатель: ${makeDisplayAccountNumber(toAccountId)}`);
    } catch (error) {
      notify('error', 'Перевод не выполнен', error instanceof Error ? error.message : undefined);
    }
  }

  async function pay(payload: { accountId: string; title: string; amount: number }) {
    if (!payload.amount || payload.amount <= 0) {
      notify('error', 'Проверьте данные платежа');
      return;
    }

    try {
      const accounts = await refreshAccounts();
      let account = accounts.find((item) => item.id === payload.accountId);
      if (!account) {
        notify('error', 'Счёт не найден');
        return;
      }

      account = await prepareDemoAccountIfNeeded(account, payload.amount);
      if (account.balance < payload.amount) {
        notify('error', 'Недостаточно средств', 'Сначала пополните выбранный счёт');
        return;
      }

      const updatedAccount = personalizeAccountName(await coreApi.withdraw(account.id, payload.amount), auth.email);
      updateAccountInState(updatedAccount);
      const freshAccounts = await refreshAccounts();
      await refreshHistory(freshAccounts);
      notify('success', 'Платёж выполнен');
    } catch (error) {
      notify('error', 'Платёж не выполнен', error instanceof Error ? error.message : undefined);
    }
  }

  async function exchange(payload: { fromAccountId: string; toCurrency: CurrencyCode; amount: number }) {
    if (!payload.amount || payload.amount <= 0) {
      notify('error', 'Проверьте параметры обмена');
      return;
    }

    if (!isSupportedBackendCurrency(payload.toCurrency)) {
      notify('error', 'Эта валюта сейчас недоступна');
      return;
    }

    try {
      await refreshRates();
      let accounts = await refreshAccounts();
      let account = accounts.find((item) => item.id === payload.fromAccountId);
      if (!account) {
        notify('error', 'Счёт списания не найден');
        return;
      }

      if (account.currency === payload.toCurrency) {
        notify('error', 'Выберите другую валюту');
        return;
      }

      account = await prepareDemoAccountIfNeeded(account, payload.amount);
      if (account.balance < payload.amount) {
        notify('error', 'Недостаточно средств', 'Сначала пополните выбранный счёт');
        return;
      }

      let targetAccount = accounts.find(
        (item) => item.status === 'active' && item.currency === payload.toCurrency && item.type === account.type,
      );

      if (!targetAccount) {
        targetAccount = personalizeAccountName(await coreApi.createAccount({ currency: payload.toCurrency, type: account.type }), auth.email);
        accounts = replaceAccount(accounts, targetAccount);
      }

      const result = await currencyApi.convert({
        from: account.currency,
        to: payload.toCurrency,
        amount: payload.amount,
      });

      await coreApi.transfer({
        fromAccountId: account.id,
        toAccountId: targetAccount.id,
        amount: payload.amount,
        currency: account.currency,
      });

      const freshAccounts = await refreshAccounts();
      await refreshHistory(freshAccounts);
      notify('success', 'Обмен выполнен', `Зачислено примерно ${result.result.toFixed(2)} ${payload.toCurrency}`);
    } catch (error) {
      notify('error', 'Обмен не выполнен', error instanceof Error ? error.message : undefined);
    }
  }

  async function submitAction(action: Action, payload: Record<string, string | number>) {
    if (action === 'openAccount') {
      await openAccount({
        name: String(payload.name || 'Новый счёт'),
        currency: String(payload.currency || 'RUB') as CurrencyCode,
        type: String(payload.type || 'debit') as Account['type'],
      });
      return;
    }

    if (action === 'topup') {
      await topUp(String(payload.accountId), Number(payload.amount));
      return;
    }

    if (action === 'transfer') {
      await transfer({
        fromAccountId: String(payload.accountId),
        toAccountNumber: String(payload.toAccountNumber),
        amount: Number(payload.amount),
        transferMode: String(payload.transferMode || 'own'),
      });
      return;
    }

    if (action === 'pay') {
      await pay({
        accountId: String(payload.accountId),
        title: String(payload.title || 'Оплата услуг'),
        amount: Number(payload.amount),
      });
      return;
    }

    if (action === 'exchange') {
      await exchange({
        fromAccountId: String(payload.accountId),
        toCurrency: String(payload.toCurrency || 'USD') as CurrencyCode,
        amount: Number(payload.amount),
      });
    }
  }

  function sendAssistantMessage(text: string) {
    if (!text.trim()) return;

    const userMessage: AssistantMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      text,
      createdAt: new Date().toISOString(),
    };

    const lower = text.toLowerCase();
    const total = getTotalBalance(state.accounts, state.rates);
    let answer = 'Я могу подсказать информацию по вашим счетам, операциям и валюте.';

    if (lower.includes('расход') || lower.includes('операц')) {
      const last = state.transactions
        .slice(0, 4)
        .map((tx) => `${tx.title}: ${tx.amount} ${tx.currency}`)
        .join('; ');
      answer = last ? `Последние операции: ${last}.` : 'История операций пока пустая.';
    }

    if (lower.includes('баланс') || lower.includes('деньг')) {
      answer = `Общий баланс по активным счетам примерно ${Math.round(total).toLocaleString('ru-RU')} ₽.`;
    }

    if (lower.includes('валют') || lower.includes('курс')) {
      const rates = state.rates.map((rate) => `${rate.code}: ${rate.sell.toFixed(2)} ₽`).join(', ');
      answer = `Текущие курсы валют: ${rates}.`;
    }

    const assistantMessage: AssistantMessage = {
      id: crypto.randomUUID(),
      role: 'assistant',
      text: answer,
      createdAt: new Date().toISOString(),
    };

    setState((current) => ({
      ...current,
      assistantMessages: [...current.assistantMessages, userMessage, assistantMessage],
    }));
  }

  const summary = useMemo(
    () => ({
      totalBalance: getTotalBalance(state.accounts, state.rates),
      activeAccounts: state.accounts.filter((account) => account.status === 'active'),
      latestTransactions: state.transactions.slice(0, 6),
    }),
    [state.accounts, state.rates, state.transactions],
  );

  return {
    state,
    auth,
    summary,
    toasts,
    notify,
    removeToast,
    login,
    register,
    loginDemo,
    logout,
    resetDemoData,
    setTheme,
    updateProfile,
    openAccount,
    closeAccount,
    topUp,
    transfer,
    pay,
    exchange,
    submitAction,
    sendAssistantMessage,
  };
}
