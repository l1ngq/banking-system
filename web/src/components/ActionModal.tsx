import { FormEvent, useMemo, useState } from 'react';
import type { Account, Action, CurrencyCode } from '../types/banking';

interface ActionModalProps {
  action: Action | null;
  accounts: Account[];
  onClose: () => void;
  onSubmit: (action: Action, payload: Record<string, string | number>) => void | Promise<void>;
}

interface ActionModalFormProps extends ActionModalProps {
  action: Action;
}

const titles: Record<Action, string> = {
  topup: 'Пополнить счёт',
  transfer: 'Перевести деньги',
  pay: 'Оплатить услугу',
  exchange: 'Обменять валюту',
  openAccount: 'Открыть счёт',
};

function getPreferredAccount(accounts: Account[]) {
  return [...accounts].sort((left, right) => {
    if (left.currency === 'RUB' && right.currency !== 'RUB') return -1;
    if (left.currency !== 'RUB' && right.currency === 'RUB') return 1;
    return right.balance - left.balance;
  })[0];
}

function createInitialPayload(accounts: Account[]) {
  const activeAccounts = accounts.filter((account) => account.status === 'active');
  const preferredAccount = getPreferredAccount(activeAccounts);
  const firstAccountId = preferredAccount?.id ?? activeAccounts[0]?.id ?? accounts[0]?.id ?? '';
  const secondAccountId = activeAccounts.find((account) => account.id !== firstAccountId)?.id ?? '';

  return {
    accountId: firstAccountId,
    transferMode: 'own',
    toAccountNumber: secondAccountId,
    currency: 'RUB',
    type: 'debit',
    toCurrency: preferredAccount?.currency === 'USD' ? 'RUB' : 'USD',
  };
}

function ActionModalForm({ action, accounts, onClose, onSubmit }: ActionModalFormProps) {
  const [payload, setPayload] = useState<Record<string, string>>(() => createInitialPayload(accounts));
  const [submitting, setSubmitting] = useState(false);

  const activeAccounts = useMemo(
    () => accounts.filter((account) => account.status === 'active'),
    [accounts],
  );

  const selectedAccount = activeAccounts.find((account) => account.id === payload.accountId);
  const destinationAccounts = activeAccounts.filter((account) => account.id !== payload.accountId);
  const isExternalTransfer = action === 'transfer' && payload.transferMode === 'external';

  function update(key: string, value: string) {
    setPayload((current) => {
      const next = { ...current, [key]: value };

      if (key === 'accountId' && action === 'transfer' && next.transferMode !== 'external') {
        next.toAccountNumber = activeAccounts.find((account) => account.id !== value)?.id ?? '';
      }

      if (key === 'transferMode') {
        next.toAccountNumber = value === 'external'
          ? ''
          : activeAccounts.find((account) => account.id !== next.accountId)?.id ?? '';
      }

      if (key === 'accountId' && action === 'exchange') {
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
      await onSubmit(action, payload);
      onClose();
    } finally {
      setSubmitting(false);
    }
  }

  const accountSelect = (
    <label>
      Счёт
      <select value={payload.accountId ?? ''} onChange={(event) => update('accountId', event.target.value)} disabled={submitting}>
        {activeAccounts.map((account) => (
          <option key={account.id} value={account.id}>
            {account.name} · {account.number.slice(-4)} · {account.currency}
          </option>
        ))}
      </select>
    </label>
  );

  return (
    <div className="modal">
      <button className="modal__backdrop" type="button" onClick={onClose} aria-label="Закрыть" />
      <form className="modal__content form" onSubmit={submit}>
        <div className="modal__head">
          <h2>{titles[action]}</h2>
          <button type="button" onClick={onClose}>×</button>
        </div>

        {action === 'openAccount' ? (
          <>
            <label>
              Название счёта
              <input value={payload.name ?? ''} onChange={(event) => update('name', event.target.value)} placeholder="Например, Валютный счёт" disabled={submitting} />
            </label>
            <div className="form__grid">
              <label>
                Валюта
                <select value={payload.currency ?? 'RUB'} onChange={(event) => update('currency', event.target.value)} disabled={submitting}>
                  <option value="RUB">RUB</option>
                  <option value="USD">USD</option>
                  <option value="EUR">EUR</option>
                </select>
              </label>
              <label>
                Тип
                <select value={payload.type ?? 'debit'} onChange={(event) => update('type', event.target.value)} disabled={submitting}>
                  <option value="debit">Текущий</option>
                  <option value="saving">Накопительный</option>
                </select>
              </label>
            </div>
          </>
        ) : (
          <>
            {accountSelect}
            <label>
              Сумма
              <input type="number" min="1" step="0.01" value={payload.amount ?? ''} onChange={(event) => update('amount', event.target.value)} placeholder="1000" disabled={submitting} />
            </label>
          </>
        )}

        {action === 'transfer' && (
          <>
            <div className="form__grid">
              <button type="button" className={payload.transferMode === 'own' ? 'button-primary' : 'text-button'} onClick={() => update('transferMode', 'own')} disabled={submitting}>Между своими счетами</button>
              <button type="button" className={payload.transferMode === 'external' ? 'button-primary' : 'text-button'} onClick={() => update('transferMode', 'external')} disabled={submitting}>По номеру счёта</button>
            </div>
            {isExternalTransfer ? (
              <>
                <label>
                  Номер счёта получателя
                  <input value={payload.toAccountNumber ?? ''} onChange={(event) => update('toAccountNumber', event.target.value)} placeholder="Например, 40817810000000000000" disabled={submitting} />
                </label>
                <p className="form__hint">Это черновик перевода: заявка появится в истории, а фактическое списание будет доступно после реализации на стороне банка.</p>
              </>
            ) : destinationAccounts.length > 0 ? (
              <label>
                Счёт получателя
                <select value={payload.toAccountNumber ?? ''} onChange={(event) => update('toAccountNumber', event.target.value)} disabled={submitting}>
                  {destinationAccounts.map((account) => (
                    <option key={account.id} value={account.id}>
                      {account.name} · {account.number.slice(-4)} · {account.currency}
                    </option>
                  ))}
                </select>
              </label>
            ) : (
              <p className="form__hint">Откройте второй счёт, чтобы выполнить перевод между своими счетами.</p>
            )}
          </>
        )}

        {action === 'pay' && (
          <label>
            Назначение
            <input value={payload.title ?? ''} onChange={(event) => update('title', event.target.value)} placeholder="Оплата услуг" disabled={submitting} />
          </label>
        )}

        {action === 'exchange' && (
          <label>
            Получить валюту
            <select value={(payload.toCurrency as CurrencyCode) ?? 'USD'} onChange={(event) => update('toCurrency', event.target.value)} disabled={submitting}>
              <option value="USD" disabled={selectedAccount?.currency === 'USD'}>USD</option>
              <option value="EUR" disabled={selectedAccount?.currency === 'EUR'}>EUR</option>
              <option value="RUB" disabled={selectedAccount?.currency === 'RUB'}>RUB</option>
            </select>
          </label>
        )}

        <button className="button-primary" type="submit" disabled={submitting}>{submitting ? 'Выполняем…' : 'Выполнить'}</button>
      </form>
    </div>
  );
}

function ActionModal({ action, accounts, onClose, onSubmit }: ActionModalProps) {
  if (!action) return null;

  const modalKey = `${action}-${accounts.map((account) => `${account.id}:${account.status}`).join('|')}`;

  return (
    <ActionModalForm
      key={modalKey}
      action={action}
      accounts={accounts}
      onClose={onClose}
      onSubmit={onSubmit}
    />
  );
}

export default ActionModal;
