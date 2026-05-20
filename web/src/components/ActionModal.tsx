import { FormEvent, useEffect, useMemo, useState } from 'react';
import type { Account, Action, CurrencyCode } from '../types/banking';

interface ActionModalProps {
  action: Action | null;
  accounts: Account[];
  onClose: () => void;
  onSubmit: (action: Action, payload: Record<string, string | number>) => void | Promise<void>;
}

const titles: Record<Action, string> = {
  topup: 'Пополнить счёт', transfer: 'Перевести деньги', pay: 'Оплатить услугу', exchange: 'Обменять валюту', openAccount: 'Открыть счёт',
};

function ActionModal({ action, accounts, onClose, onSubmit }: ActionModalProps) {
  const [payload, setPayload] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  const activeAccounts = useMemo(
    () => accounts.filter((account) => account.status === 'active'),
    [accounts],
  );

  useEffect(() => {
    const preferredAccount = [...activeAccounts].sort((left, right) => {
      if (left.currency === 'RUB' && right.currency !== 'RUB') return -1;
      if (left.currency !== 'RUB' && right.currency === 'RUB') return 1;
      return right.balance - left.balance;
    })[0];

    const firstAccount = preferredAccount?.id ?? activeAccounts[0]?.id ?? accounts[0]?.id ?? '';
    const secondAccount = activeAccounts.find((account) => account.id !== firstAccount)?.id ?? '';

    // Значения формы должны сбрасываться каждый раз при открытии нового действия.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setPayload({
      accountId: firstAccount,
      toAccountNumber: secondAccount,
      currency: 'RUB',
      type: 'debit',
      toCurrency: preferredAccount?.currency === 'USD' ? 'RUB' : 'USD',
    });
    setSubmitting(false);
  }, [action, accounts, activeAccounts]);

  if (!action) return null;
  const activeAction = action;
  const selectedAccount = activeAccounts.find((account) => account.id === payload.accountId);
  const destinationAccounts = activeAccounts.filter((account) => account.id !== payload.accountId);

  function update(key: string, value: string) {
    setPayload((current) => {
      const next = { ...current, [key]: value };

      if (key === 'accountId' && activeAction === 'transfer') {
        const nextDestination = activeAccounts.find((account) => account.id !== value)?.id ?? '';
        next.toAccountNumber = nextDestination;
      }

      if (key === 'accountId' && activeAction === 'exchange') {
        const account = activeAccounts.find((item) => item.id === value);
        if (account && account.currency === next.toCurrency) {
          next.toCurrency = account.currency === 'RUB' ? 'USD' : 'RUB';
        }
      }

      return next;
    });
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (submitting) return;

    setSubmitting(true);
    try {
      await onSubmit(activeAction, payload);
      onClose();
    } finally {
      setSubmitting(false);
    }
  }

  const accountSelect = (
    <label>Счёт
      <select value={payload.accountId ?? ''} onChange={(event) => update('accountId', event.target.value)} disabled={submitting}>
        {activeAccounts.map((account) => <option key={account.id} value={account.id}>{account.name} · {account.number.slice(-4)} · {account.currency}</option>)}
      </select>
    </label>
  );

  return (
    <div className="modal">
      <button className="modal__backdrop" type="button" onClick={onClose} aria-label="Закрыть" />
      <form className="modal__content form" onSubmit={submit}>
        <div className="modal__head"><h2>{titles[activeAction]}</h2><button type="button" onClick={onClose}>×</button></div>
        {activeAction === 'openAccount' ? (
          <>
            <label>Название счёта<input value={payload.name ?? ''} onChange={(event) => update('name', event.target.value)} placeholder="Например, Валютный счёт" disabled={submitting} /></label>
            <div className="form__grid">
              <label>Валюта<select value={payload.currency ?? 'RUB'} onChange={(event) => update('currency', event.target.value)} disabled={submitting}><option value="RUB">RUB</option><option value="USD">USD</option><option value="EUR">EUR</option></select></label>
              <label>Тип<select value={payload.type ?? 'debit'} onChange={(event) => update('type', event.target.value)} disabled={submitting}><option value="debit">Текущий</option><option value="saving">Накопительный</option></select></label>
            </div>
          </>
        ) : (
          <>{accountSelect}<label>Сумма<input type="number" min="1" step="0.01" value={payload.amount ?? ''} onChange={(event) => update('amount', event.target.value)} placeholder="1000" disabled={submitting} /></label></>
        )}
        {activeAction === 'transfer' && (
          destinationAccounts.length > 0 ? (
            <label>Счёт получателя
              <select value={payload.toAccountNumber ?? ''} onChange={(event) => update('toAccountNumber', event.target.value)} disabled={submitting}>
                {destinationAccounts.map((account) => <option key={account.id} value={account.id}>{account.name} · {account.number.slice(-4)} · {account.currency}</option>)}
              </select>
            </label>
          ) : (
            <label>Счёт получателя<input value={payload.toAccountNumber ?? ''} onChange={(event) => update('toAccountNumber', event.target.value)} placeholder="ID или номер счёта" disabled={submitting} /></label>
          )
        )}
        {activeAction === 'pay' && <label>Назначение<input value={payload.title ?? ''} onChange={(event) => update('title', event.target.value)} placeholder="Оплата услуг" disabled={submitting} /></label>}
        {activeAction === 'exchange' && <label>Получить валюту<select value={(payload.toCurrency as CurrencyCode) ?? 'USD'} onChange={(event) => update('toCurrency', event.target.value)} disabled={submitting}><option value="USD" disabled={selectedAccount?.currency === 'USD'}>USD</option><option value="EUR" disabled={selectedAccount?.currency === 'EUR'}>EUR</option><option value="RUB" disabled={selectedAccount?.currency === 'RUB'}>RUB</option></select></label>}
        <button className="button-primary" type="submit" disabled={submitting}>{submitting ? 'Выполняем…' : 'Выполнить'}</button>
      </form>
    </div>
  );
}

export default ActionModal;
