import { useEffect, useMemo, useState } from 'react';

import { getDisplayNameByEmail } from '../api/config';
import { coreApi } from '../api/coreApi';
import { currencyApi } from '../api/currencyApi';
import { operationsApi } from '../api/operationsApi';
import { createBankingStateForEmail } from '../data/mockData';
import type { Action, Account, BankingState, CurrencyCode, Theme, Transaction } from '../types/banking';
import { getTotalBalance } from '../utils/calculations';
import { formatMoney } from '../utils/formatters';
import {
  isSupportedBackendCurrency,
  isValidExternalAccountNumber,
  replaceAccount,
  uniqueTransactions,
} from '../services/bankingHelpers';
import { readBankingState, saveBankingState } from '../services/bankingStorage';

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

export interface PayPayload {
  accountId: string;
  title: string;
  amount: number;
  category?: string;
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : undefined;
}

/** Сервер ещё без раздела «Операции» — тогда платим обычным списанием. */
function isMissingEndpoint(error: unknown) {
  return /404|not found|no static resource/i.test(errorMessage(error) ?? '');
}

function friendlyAuthError(error: unknown, fallback: string) {
  const message = errorMessage(error) ?? '';
  if (/login failed|401|bad credentials/i.test(message)) return 'Неверная почта или пароль.';
  if (/exist|already|уже зарегистр/i.test(message)) return 'Пользователь с такой почтой уже зарегистрирован.';
  return message || fallback;
}

export function useBankingState() {
  const [state, setState] = useState<BankingState>(() => readBankingState());
  const [auth, setAuth] = useState<AuthViewState>({ status: 'checking', email: null, error: null });
  const [toasts, setToasts] = useState<Toast[]>([]);

  useEffect(() => {
    if (auth.status === 'authenticated' && auth.email) {
      saveBankingState(auth.email, state);
    }

    document.documentElement.dataset.theme = state.profile.theme;
  }, [auth.email, auth.status, state]);

  function notify(type: ToastType, title: string, text?: string) {
    const toast: Toast = { id: crypto.randomUUID(), type, title, text };
    setToasts((current) => [...current, toast]);

    window.setTimeout(() => {
      setToasts((current) => current.filter((item) => item.id !== toast.id));
    }, type === 'error' ? 6000 : 3600);
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

  async function refreshAccounts() {
    const accounts = await coreApi.getAccounts();
    setState((current) => ({ ...current, accounts }));
    return accounts;
  }

  async function refreshHistory(accounts: Account[]) {
    const histories = await Promise.all(
      accounts
        .filter((account) => account.status === 'active')
        .map((account) => coreApi.getHistory(account, accounts).catch(() => [] as Transaction[])),
    );
    const backendTransactions = histories.flat();

    setState((current) => ({
      ...current,
      transactions: uniqueTransactions([
        ...backendTransactions,
        ...current.transactions.filter((transaction) => transaction.status === 'pending'),
      ]),
    }));
  }

  async function refreshRates() {
    try {
      const rates = await currencyApi.getRates();
      setState((current) => ({ ...current, rates }));
      return rates;
    } catch {
      setState((current) => ({ ...current, rates: [] }));
      return [];
    }
  }

  async function loadBackendData(emailOverride?: string | null) {
    const profile = await coreApi.getProfile();
    const profileEmail = profile.email ?? emailOverride ?? auth.email;

    setState((current) => ({
      ...current,
      profile: {
        ...current.profile,
        ...profile,
        email: profileEmail ?? current.profile.email,
        fullName: current.profile.fullName || getDisplayNameByEmail(profileEmail, 'Пользователь'),
      },
    }));

    await refreshRates();
    const accounts = await refreshAccounts();
    await refreshHistory(accounts);
  }

  /** Перезагрузить счета, курсы и историю (например, после оплаты в разделе «Операции»). */
  async function refresh() {
    try {
      await loadBackendData();
    } catch (error) {
      notify('error', 'Не удалось обновить данные', errorMessage(error));
    }
  }

  useEffect(() => {
    let cancelled = false;

    async function run() {
      try {
        const session = await coreApi.getAuthState();
        if (cancelled) return;

        if (!session.authenticated) {
          setAuth({ status: 'guest', email: null, error: null });
          return;
        }

        const sessionEmail = session.email;
        const saved = readBankingState(sessionEmail);
        setState(saved);
        setAuth({ status: 'authenticated', email: sessionEmail, error: null });

        const profile = await coreApi.getProfile();
        if (cancelled) return;

        const profileEmail = profile.email ?? sessionEmail;
        setState((current) => ({
          ...current,
          profile: {
            ...current.profile,
            ...profile,
            email: profileEmail ?? current.profile.email,
            fullName: current.profile.fullName || getDisplayNameByEmail(profileEmail, 'Пользователь'),
          },
        }));

        const rates = await currencyApi.getRates().catch(() => []);
        if (cancelled) return;
        setState((current) => ({ ...current, rates }));

        const accounts = await coreApi.getAccounts();
        if (cancelled) return;
        setState((current) => ({ ...current, accounts }));

        const histories = await Promise.all(
          accounts
            .filter((account) => account.status === 'active')
            .map((account) => coreApi.getHistory(account, accounts).catch(() => [] as Transaction[])),
        );
        if (cancelled) return;

        setState((current) => ({
          ...current,
          transactions: uniqueTransactions([
            ...histories.flat(),
            ...current.transactions.filter((transaction) => transaction.status === 'pending'),
          ]),
        }));
      } catch {
        if (!cancelled) {
          setAuth({ status: 'guest', email: null, error: null });
        }
      }
    }

    void run();

    return () => {
      cancelled = true;
    };
  }, []);

  async function login(email: string, password: string) {
    setAuth({ status: 'checking', email, error: null });

    try {
      await coreApi.login(email, password);
      const session = await coreApi.getAuthState();
      if (!session.authenticated) {
        throw new Error('Login failed: 401');
      }
      const sessionEmail = session.email ?? email;
      setState(readBankingState(sessionEmail));
      setAuth({ status: 'authenticated', email: sessionEmail, error: null });
      await loadBackendData(sessionEmail);
      notify('success', 'Добро пожаловать!', 'Вы вошли в личный кабинет');
    } catch (error) {
      setAuth({ status: 'guest', email: null, error: friendlyAuthError(error, 'Не удалось войти. Проверьте почту и пароль.') });
    }
  }

  async function register(email: string, password: string) {
    setAuth({ status: 'checking', email, error: null });

    try {
      await coreApi.register(email, password);
      await coreApi.login(email, password);
      const session = await coreApi.getAuthState();
      const sessionEmail = session.email ?? email;
      setState(readBankingState(sessionEmail));
      setAuth({ status: 'authenticated', email: sessionEmail, error: null });
      await loadBackendData(sessionEmail);
      notify('success', 'Аккаунт создан', 'Откройте первый счёт, чтобы начать');
    } catch (error) {
      setAuth({ status: 'guest', email: null, error: friendlyAuthError(error, 'Не удалось создать аккаунт.') });
    }
  }

  async function logout() {
    try {
      await coreApi.logout();
    } catch (error) {
      void error;
    }

    setAuth({ status: 'guest', email: null, error: null });
    setState(createBankingStateForEmail(null));
    notify('info', 'Вы вышли из личного кабинета');
  }

  function setTheme(theme: Theme) {
    setState((current) => ({ ...current, profile: { ...current.profile, theme } }));
  }

  function toggleHideBalance() {
    setState((current) => ({
      ...current,
      profile: { ...current.profile, hideBalance: !current.profile.hideBalance },
    }));
  }

  function updateProfile(payload: Partial<BankingState['profile']>) {
    setState((current) => ({ ...current, profile: { ...current.profile, ...payload } }));
    notify('success', 'Профиль сохранён');
  }

  async function renameAccount(accountId: string, name: string) {
    const trimmed = name.trim();
    try {
      const account = await coreApi.renameAccount(accountId, trimmed);
      updateAccountInState(account);
      notify('success', 'Название счёта сохранено');
      return true;
    } catch (error) {
      notify('error', 'Не удалось переименовать счёт', errorMessage(error));
      return false;
    }
  }

  async function openAccount(payload: { name: string; currency: CurrencyCode; type: Account['type'] }) {
    if (!isSupportedBackendCurrency(payload.currency)) {
      notify('error', 'Эта валюта сейчас недоступна');
      return false;
    }

    try {
      const name = payload.name.trim();
      const account = await coreApi.createAccount({ currency: payload.currency, type: payload.type, name });
      updateAccountInState(account);
      notify('success', 'Счёт открыт', account.name);
      return true;
    } catch (error) {
      notify('error', 'Не удалось открыть счёт', errorMessage(error));
      return false;
    }
  }

  async function closeAccount(accountId: string) {
    const accounts = await refreshAccounts().catch(() => state.accounts);
    const account = accounts.find((item) => item.id === accountId);
    if (account && account.balance > 0) {
      notify('error', 'Нельзя закрыть счёт с остатком', 'Сначала переведите деньги на другой счёт');
      return false;
    }

    try {
      await coreApi.closeAccount(accountId);
      await refreshAccounts();
      notify('info', 'Счёт закрыт', account?.name);
      return true;
    } catch (error) {
      notify('error', 'Не удалось закрыть счёт', errorMessage(error));
      return false;
    }
  }

  async function topUp(accountId: string, amount: number) {
    if (!amount || amount <= 0) {
      notify('error', 'Введите сумму больше нуля');
      return false;
    }

    try {
      const accounts = await refreshAccounts().catch(() => state.accounts);
      const account = accounts.find((item) => item.id === accountId);
      if (!account) {
        notify('error', 'Счёт не найден');
        return false;
      }

      const updatedAccount = await coreApi.deposit(account.id, amount);
      updateAccountInState(updatedAccount);

      const freshAccounts = await refreshAccounts();
      await refreshHistory(freshAccounts);
      notify('success', 'Счёт пополнен', `+${formatMoney(amount, account.currency)} на «${account.name}»`);
      return true;
    } catch (error) {
      notify('error', 'Пополнение не выполнено', errorMessage(error));
      return false;
    }
  }

  async function transfer(payload: { fromAccountId: string; toAccountNumber: string; amount: number; transferMode?: string }) {
    if (!payload.amount || payload.amount <= 0 || payload.toAccountNumber.trim().length < 1) {
      notify('error', 'Проверьте данные перевода');
      return false;
    }

    try {
      await refreshRates();
      const accounts = await refreshAccounts();
      const account = accounts.find((item) => item.id === payload.fromAccountId);
      if (!account) {
        notify('error', 'Счёт отправителя не найден');
        return false;
      }

      if (account.balance < payload.amount) {
        notify('error', 'Недостаточно средств', 'Сначала пополните выбранный счёт');
        return false;
      }

      const rawRecipientNumber = payload.toAccountNumber.trim();
      let recipientAccountNumber = rawRecipientNumber.replace(/\s/g, '');

      if (payload.transferMode !== 'external') {
        const recipientAccount = accounts.find((item) => {
          const compactNumber = item.number.replace(/\s/g, '');
          return item.id === rawRecipientNumber
            || item.number === rawRecipientNumber
            || compactNumber === recipientAccountNumber;
        });

        if (!recipientAccount) {
          notify('error', 'Счёт получателя не найден');
          return false;
        }

        recipientAccountNumber = recipientAccount.number.replace(/\s/g, '');
      }

      if (!isValidExternalAccountNumber(recipientAccountNumber)) {
        notify('error', 'Номер счёта должен состоять из 20 цифр');
        return false;
      }

      await coreApi.transfer({
        fromAccountId: account.id,
        toAccountNumber: recipientAccountNumber,
        amount: payload.amount,
        currency: account.currency,
      });

      const freshAccounts = await refreshAccounts();
      await refreshHistory(freshAccounts);
      notify('success', 'Перевод отправлен', `${formatMoney(payload.amount, account.currency)} на счёт •${recipientAccountNumber.slice(-4)}`);
      return true;
    } catch (error) {
      notify('error', 'Перевод не выполнен', errorMessage(error));
      return false;
    }
  }

  async function pay(payload: PayPayload) {
    if (!payload.amount || payload.amount <= 0) {
      notify('error', 'Проверьте данные платежа');
      return false;
    }

    try {
      const accounts = await refreshAccounts();
      const account = accounts.find((item) => item.id === payload.accountId);
      if (!account) {
        notify('error', 'Счёт не найден');
        return false;
      }

      if (account.balance < payload.amount) {
        notify('error', 'Недостаточно средств', 'Сначала пополните выбранный счёт');
        return false;
      }

      try {
        await operationsApi.createPayment({
          accountId: account.id,
          amount: payload.amount,
          merchant: payload.title,
          category: payload.category,
        });
      } catch (error) {
        if (!isMissingEndpoint(error)) {
          throw error;
        }
        const updatedAccount = await coreApi.withdraw(account.id, payload.amount);
        updateAccountInState(updatedAccount);
      }

      const freshAccounts = await refreshAccounts();
      await refreshHistory(freshAccounts);
      notify('success', 'Оплата прошла', `${payload.title}: ${formatMoney(payload.amount, account.currency)}`);
      return true;
    } catch (error) {
      notify('error', 'Платёж не выполнен', errorMessage(error));
      return false;
    }
  }

  async function exchange(payload: { fromAccountId: string; toCurrency: CurrencyCode; amount: number }) {
    if (!payload.amount || payload.amount <= 0) {
      notify('error', 'Проверьте параметры обмена');
      return false;
    }

    if (!isSupportedBackendCurrency(payload.toCurrency)) {
      notify('error', 'Эта валюта сейчас недоступна');
      return false;
    }

    try {
      await refreshRates();
      let accounts = await refreshAccounts();
      const account = accounts.find((item) => item.id === payload.fromAccountId);
      if (!account) {
        notify('error', 'Счёт списания не найден');
        return false;
      }

      if (account.currency === payload.toCurrency) {
        notify('error', 'Выберите другую валюту');
        return false;
      }

      if (account.balance < payload.amount) {
        notify('error', 'Недостаточно средств', 'Сначала пополните выбранный счёт');
        return false;
      }

      let targetAccount = accounts.find(
        (item) => item.status === 'active' && item.currency === payload.toCurrency && item.type === account.type,
      );

      if (!targetAccount) {
        targetAccount = await coreApi.createAccount({ currency: payload.toCurrency, type: account.type });
        accounts = replaceAccount(accounts, targetAccount);
      }

      const result = await currencyApi.convert({
        from: account.currency,
        to: payload.toCurrency,
        amount: payload.amount,
      });

      await coreApi.transfer({
        fromAccountId: account.id,
        toAccountNumber: targetAccount.number,
        amount: payload.amount,
        currency: account.currency,
      });

      const freshAccounts = await refreshAccounts();
      await refreshHistory(freshAccounts);
      notify('success', 'Обмен выполнен', `Зачислено около ${formatMoney(Number(result.result.toFixed(2)), payload.toCurrency)}`);
      return true;
    } catch (error) {
      notify('error', 'Обмен не выполнен', errorMessage(error));
      return false;
    }
  }

  async function submitAction(action: Action, payload: Record<string, string | number>) {
    if (action === 'openAccount') {
      return openAccount({
        name: String(payload.name || ''),
        currency: String(payload.currency || 'RUB') as CurrencyCode,
        type: String(payload.type || 'debit') as Account['type'],
      });
    }

    if (action === 'topup') {
      return topUp(String(payload.accountId), Number(payload.amount));
    }

    if (action === 'transfer') {
      return transfer({
        fromAccountId: String(payload.accountId),
        toAccountNumber: String(payload.toAccountNumber),
        amount: Number(payload.amount),
        transferMode: String(payload.transferMode || 'own'),
      });
    }

    if (action === 'pay') {
      return pay({
        accountId: String(payload.accountId),
        title: String(payload.title || 'Оплата услуг'),
        amount: Number(payload.amount),
        category: payload.category ? String(payload.category) : undefined,
      });
    }

    return exchange({
      fromAccountId: String(payload.accountId),
      toCurrency: String(payload.toCurrency || 'USD') as CurrencyCode,
      amount: Number(payload.amount),
    });
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
    logout,
    refresh,
    setTheme,
    toggleHideBalance,
    updateProfile,
    renameAccount,
    openAccount,
    closeAccount,
    topUp,
    transfer,
    pay,
    exchange,
    submitAction,
  };
}
