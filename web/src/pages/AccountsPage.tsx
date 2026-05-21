import { useState } from 'react';
import type { Account, Action } from '../types/banking';
import { formatDate, formatMoney, maskAccountNumber } from '../utils/formatters';

interface AccountsPageProps {
  accounts: Account[];
  onAction: (action: Action) => void;
  onCloseAccount: (id: string) => void;
}

function AccountsPage({ accounts, onAction, onCloseAccount }: AccountsPageProps) {
  const [visibleAccountIds, setVisibleAccountIds] = useState<string[]>([]);
  const [copiedAccountId, setCopiedAccountId] = useState<string | null>(null);

  function toggleAccountNumber(accountId: string) {
    setVisibleAccountIds((current) =>
      current.includes(accountId)
        ? current.filter((id) => id !== accountId)
        : [...current, accountId],
    );
  }

  async function copyAccountNumber(account: Account) {
    try {
      await navigator.clipboard.writeText(account.number);
      setCopiedAccountId(account.id);

      window.setTimeout(() => {
        setCopiedAccountId((current) => (current === account.id ? null : current));
      }, 1800);
    } catch {
      setCopiedAccountId(null);
    }
  }

  return (
    <div className="page-grid">
      <section className="page-hero">
        <div>
          <p className="eyebrow">мои счета</p>
          <h2>Счета</h2>
          <p>Управляйте счетами, балансом и доступными действиями.</p>
        </div>

        <button type="button" onClick={() => onAction('openAccount')}>
          Открыть счёт
        </button>
      </section>

      <div className="accounts-list">
        {accounts.length === 0 ? (
          <section className="card success-state">
            <h2>Счетов пока нет</h2>
            <p>Откройте первый счёт, чтобы начать пользоваться личным кабинетом.</p>
            <button type="button" onClick={() => onAction('openAccount')}>
              Открыть счёт
            </button>
          </section>
        ) : (
          accounts.map((account) => {
            const isNumberVisible = visibleAccountIds.includes(account.id);
            const displayedNumber = isNumberVisible
              ? account.number
              : maskAccountNumber(account.number);

            return (
              <article
                key={account.id}
                className={`account-card ${account.status === 'closed' ? 'account-card--closed' : ''}`}
              >
                <div className="account-card__main">
                  <span className="badge">
                    {account.type === 'saving' ? 'Накопительный' : 'Текущий'}
                  </span>

                  <h3>{account.name}</h3>

                  <div className="account-card__number-row">
                    <p className="account-number">
                      {displayedNumber} · открыт {formatDate(account.openedAt)}
                    </p>

                    <div className="account-card__number-actions">
                      <button
                        type="button"
                        className="text-button text-button--small"
                        onClick={() => toggleAccountNumber(account.id)}
                      >
                        {isNumberVisible ? 'Скрыть номер' : 'Показать номер'}
                      </button>

                      <button
                        type="button"
                        className="text-button text-button--small"
                        onClick={() => copyAccountNumber(account)}
                      >
                        {copiedAccountId === account.id ? 'Скопировано' : 'Скопировать'}
                      </button>
                    </div>
                  </div>

                  <span>
                    {account.currency} · {account.status === 'active' ? 'активен' : 'закрыт'}
                  </span>
                </div>

                <div className="account-card__side">
                  <strong>{formatMoney(account.balance, account.currency)}</strong>

                  {account.status === 'active' && (
                    <button type="button" onClick={() => onCloseAccount(account.id)}>
                      Закрыть
                    </button>
                  )}
                </div>
              </article>
            );
          })
        )}
      </div>

      <section className="card">
        <div className="section-head">
          <h2>Действия</h2>
        </div>

        <div className="filter-tabs">
          <button type="button" onClick={() => onAction('topup')}>
            Пополнить
          </button>
          <button type="button" onClick={() => onAction('pay')}>
            Списать
          </button>
          <button type="button" onClick={() => onAction('transfer')}>
            Перевести
          </button>
          <button type="button" onClick={() => onAction('exchange')}>
            Обменять
          </button>
        </div>
      </section>
    </div>
  );
}

export default AccountsPage;