import { useEffect, useMemo, useState } from 'react';

import { getDisplayNameByEmail } from '../api/config';
import { coreApi } from '../api/coreApi';
import { currencyApi } from '../api/currencyApi';
import { createBankingStateForEmail } from '../data/mockData';
import type { Action, Account, BankingState, CurrencyCode, Theme, Transaction } from '../types/banking';
import { getTotalBalance } from '../utils/calculations';
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

  async function refreshAccounts() {
    const accounts = await coreApi.getAccounts();
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

  async function loadBackendData(showSuccessToast = false, emailOverride?: string | null) {
    const profile = await coreApi.getProfile();
    const profileEmail = profile.email ?? emailOverride ?? auth.email;

    setState((current) => ({
      ...current,
      profile: {
        ...current.profile,
        ...profile,
        email: profileEmail ?? current.profile.email,
        fullName: getDisplayNameByEmail(profileEmail, current.profile.fullName),
      },
    }));

    await refreshRates();
    const accounts = await refreshAccounts();
    await refreshHistory(accounts);

    if (showSuccessToast) {
      notify('success', 'Данные обновлены');
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
        setState(readBankingState(sessionEmail));
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
            fullName: getDisplayNameByEmail(profileEmail, current.profile.fullName),
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
            .map((account) => coreApi.getHistory(account).catch(() => [] as Transaction[])),
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
      const sessionEmail = session.email ?? email;
      setState(readBankingState(sessionEmail));
      setAuth({ status: 'authenticated', email: sessionEmail, error: null });
      await loadBackendData(true, sessionEmail);
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
      setState(readBankingState(sessionEmail));
      setAuth({ status: 'authenticated', email: sessionEmail, error: null });
      await loadBackendData(true, sessionEmail);
      notify('success', 'Аккаунт создан');
    } catch (error) {
      setAuth({
        status: 'guest',
        email: null,
        error: error instanceof Error ? error.message : 'Не удалось создать аккаунт.',
      });
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
      const namedAccount = payload.name ? { ...account, name: payload.name } : account;
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

      const updatedAccount = await coreApi.deposit(account.id, amount);
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
      const account = accounts.find((item) => item.id === payload.fromAccountId);
      if (!account) {
        notify('error', 'Счёт отправителя не найден');
        return;
      }

      if (account.balance < payload.amount) {
        notify('error', 'Недостаточно средств', 'Сначала пополните выбранный счёт');
        return;
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
          return;
        }

        recipientAccountNumber = recipientAccount.number.replace(/\s/g, '');
      }

      if (!isValidExternalAccountNumber(recipientAccountNumber)) {
        notify('error', 'Введите корректный номер счёта получателя');
        return;
      }

      await coreApi.transfer({
        fromAccountId: account.id,
        toAccountNumber: recipientAccountNumber,
        amount: payload.amount,
        currency: account.currency,
      });

      const freshAccounts = await refreshAccounts();
      await refreshHistory(freshAccounts);
      notify('success', 'Перевод отправлен', `Получатель: ${recipientAccountNumber}`);
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
      const account = accounts.find((item) => item.id === payload.accountId);
      if (!account) {
        notify('error', 'Счёт не найден');
        return;
      }

      if (account.balance < payload.amount) {
        notify('error', 'Недостаточно средств', 'Сначала пополните выбранный счёт');
        return;
      }

      const updatedAccount = await coreApi.withdraw(account.id, payload.amount);
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
      const account = accounts.find((item) => item.id === payload.fromAccountId);
      if (!account) {
        notify('error', 'Счёт списания не найден');
        return;
      }

      if (account.currency === payload.toCurrency) {
        notify('error', 'Выберите другую валюту');
        return;
      }

      if (account.balance < payload.amount) {
        notify('error', 'Недостаточно средств', 'Сначала пополните выбранный счёт');
        return;
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
    setTheme,
    updateProfile,
    openAccount,
    closeAccount,
    topUp,
    transfer,
    pay,
    exchange,
    submitAction,
  };
}
