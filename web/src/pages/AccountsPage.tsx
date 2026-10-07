import { useState } from 'react';

import { UiIcon } from '../components/operations/Icon';
import type { Account, Action, CurrencyCode, CurrencyRate } from '../types/banking';
import { toRub } from '../utils/calculations';
import { formatDate, formatMoney, getCurrencySymbol, groupAccountNumber, maskAccountNumber, pluralize } from '../utils/formatters';

interface AccountsPageProps {
  accounts: Account[];
  rates: CurrencyRate[];
  onAction: (action: Action, accountId?: string, mode?: 'own' | 'external') => void;
  onRequestClose: (account: Account) => void;
  onRename: (accountId: string, name: string) => Promise<boolean>;
}

const CURRENCY_TOTAL_LABEL: Record<CurrencyCode, string> = { RUB: 'В рублях', USD: 'В долларах', EUR: 'В евро' };

function AccountsPage({ accounts, rates, onAction, onRequestClose, onRename }: AccountsPageProps) {
  const [filter, setFilter] = useState<'active' | 'closed'>('active');
  const [visibleIds, setVisibleIds] = useState<string[]>([]);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draft, setDraft] = useState('');

  const active = accounts.filter((account) => account.status === 'active');
  const closed = accounts.filter((account) => account.status === 'closed');
  const shown = filter === 'active' ? active : closed;
  const currencies = (['RUB', 'USD', 'EUR'] as CurrencyCode[]).filter((code) => active.some((account) => account.currency === code));
  const totalRub = active.reduce((sum, account) => sum + toRub(account.balance, account.currency, rates), 0);

  async function copy(account: Account) {
    try {
      await navigator.clipboard.writeText(account.number);
      setCopiedId(account.id);
      window.setTimeout(() => setCopiedId((current) => (current === account.id ? null : current)), 1800);
    } catch {
      setCopiedId(null);
    }
  }

  function startRename(account: Account) {
    setEditingId(account.id);
    setDraft(account.name);
  }

  async function finishRename(save: boolean) {
    if (save && editingId && draft.trim()) {
      await onRename(editingId, draft);
    }
    setEditingId(null);
  }

  if (accounts.length === 0) {
    return (
      <div className="page">
        <section className="card">
          <div className="empty">
            <strong>Счетов пока нет</strong>
            <p>Откройте текущий счёт для повседневных операций или накопительный — с процентами на остаток.</p>
            <button type="button" className="btn btn--brand" onClick={() => onAction('openAccount')}>
              <UiIcon name="plus" size={18} strokeWidth={2.4} />
              Открыть счёт
            </button>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="toolbar">
        <div className="totals">
          {currencies.length > 1 && (
            <div className="total-chip">
              <span>Всего, в пересчёте на рубли</span>
              <strong>{formatMoney(Math.round(totalRub), 'RUB')}</strong>
            </div>
          )}
          {currencies.map((code) => (
            <div key={code} className="total-chip">
              <span>{CURRENCY_TOTAL_LABEL[code]}</span>
              <strong>
                {formatMoney(
                  active.filter((account) => account.currency === code).reduce((sum, account) => sum + account.balance, 0),
                  code,
                )}
              </strong>
            </div>
          ))}
        </div>
        <button type="button" className="btn btn--primary" onClick={() => onAction('openAccount')}>
          <UiIcon name="plus" size={18} strokeWidth={2.4} />
          Открыть счёт
        </button>
      </div>

      {closed.length > 0 && (
        <div className="segmented" role="radiogroup" aria-label="Какие счета показать">
          <button type="button" role="radio" aria-checked={filter === 'active'} className={filter === 'active' ? 'is-active' : undefined} onClick={() => setFilter('active')}>
            Активные, {active.length}
          </button>
          <button type="button" role="radio" aria-checked={filter === 'closed'} className={filter === 'closed' ? 'is-active' : undefined} onClick={() => setFilter('closed')}>
            Закрытые, {closed.length}
          </button>
        </div>
      )}

      {shown.length === 0 ? (
        <div className="empty">
          <p>{filter === 'active' ? 'Активных счетов нет — откройте новый.' : 'Закрытых счетов нет.'}</p>
        </div>
      ) : (
        <div className="accounts-grid">
          {shown.map((account) => {
            const visible = visibleIds.includes(account.id);
            const isClosed = account.status === 'closed';
            const editing = editingId === account.id;
            return (
              <article key={account.id} className={`account${isClosed ? ' is-closed' : ''}`}>
                <div className="account__head">
                  <span className="currency-bubble">{getCurrencySymbol(account.currency)}</span>
                  <div className="account__title">
                    {editing ? (
                      <input
                        className="input"
                        value={draft}
                        maxLength={40}
                        aria-label="Название счёта"
                        onChange={(event) => setDraft(event.target.value)}
                        onBlur={() => void finishRename(true)}
                        onKeyDown={(event) => {
                          if (event.key === 'Enter') void finishRename(true);
                          if (event.key === 'Escape') finishRename(false);
                        }}
                        autoFocus
                      />
                    ) : (
                      <strong>{account.name}</strong>
                    )}
                    <span>
                      {account.type === 'saving' ? `Накопительный, ${account.interestRate ?? 12.5}% годовых` : 'Текущий счёт'}
                    </span>
                  </div>
                  {isClosed ? (
                    <span className="badge badge--danger">Закрыт</span>
                  ) : (
                    !editing && (
                      <button type="button" className="icon-btn icon-btn--plain" aria-label="Переименовать счёт" title="Переименовать" onClick={() => startRename(account)}>
                        <UiIcon name="edit" size={18} />
                      </button>
                    )
                  )}
                </div>

                <div className="account__balance">{formatMoney(account.balance, account.currency)}</div>

                <div className="account__number">
                  <code>{visible ? groupAccountNumber(account.number) : maskAccountNumber(account.number)}</code>
                  {copiedId === account.id && <span className="account__copied">Скопировано</span>}
                  <button
                    type="button"
                    className="icon-btn icon-btn--plain"
                    aria-label={visible ? 'Скрыть номер счёта' : 'Показать номер счёта'}
                    title={visible ? 'Скрыть номер' : 'Показать номер'}
                    onClick={() => setVisibleIds((current) => (visible ? current.filter((id) => id !== account.id) : [...current, account.id]))}
                  >
                    <UiIcon name={visible ? 'eyeOff' : 'eye'} size={18} />
                  </button>
                  <button type="button" className="icon-btn icon-btn--plain" aria-label="Скопировать номер счёта" title="Скопировать номер" onClick={() => void copy(account)}>
                    <UiIcon name="copy" size={18} />
                  </button>
                </div>

                {!isClosed && (
                  <div className="account__actions">
                    <button type="button" className="btn btn--brand btn--sm" onClick={() => onAction('topup', account.id)}>
                      Пополнить
                    </button>
                    <button type="button" className="btn btn--sm" disabled={account.balance <= 0} onClick={() => onAction('transfer', account.id)}>
                      Перевести
                    </button>
                    <button type="button" className="btn btn--sm" disabled={account.balance <= 0} onClick={() => onAction('exchange', account.id)}>
                      Обменять
                    </button>
                  </div>
                )}

                <div className="account__footer">
                  <span>Открыт {formatDate(account.openedAt)}</span>
                  {!isClosed && (
                    <button type="button" className="link-btn" style={{ color: 'var(--negative)', fontSize: 14 }} onClick={() => onRequestClose(account)}>
                      Закрыть счёт
                    </button>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}

      <p className="hint">
        {active.length} {pluralize(active.length, 'активный счёт', 'активных счёта', 'активных счетов')}. Закрыть можно только счёт с нулевым балансом.
      </p>
    </div>
  );
}

export default AccountsPage;
