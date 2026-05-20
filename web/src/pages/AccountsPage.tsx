import type { Account, Action } from '../types/banking';
import { formatDate, formatMoney, maskAccountNumber } from '../utils/formatters';

interface AccountsPageProps { accounts: Account[]; onAction: (action: Action) => void; onCloseAccount: (id: string) => void; }

function AccountsPage({ accounts, onAction, onCloseAccount }: AccountsPageProps) {
  return (
    <div className="page-grid">
      <section className="page-hero"><div><p className="eyebrow">мои счета</p><h2>Счета</h2><p>Управляйте счетами, балансом и доступными действиями.</p></div><button type="button" onClick={() => onAction('openAccount')}>Открыть счёт</button></section>
      <div className="accounts-list">
        {accounts.map((account) => (
          <article key={account.id} className={`account-card ${account.status === 'closed' ? 'account-card--closed' : ''}`}>
            <div><span className="badge">{account.type === 'saving' ? 'Накопительный' : 'Текущий'}</span><h3>{account.name}</h3><p>{maskAccountNumber(account.number)} · открыт {formatDate(account.openedAt)}</p><span>{account.currency} · {account.status === 'active' ? 'активен' : 'закрыт'}</span></div>
            <div className="account-card__side"><strong>{formatMoney(account.balance, account.currency)}</strong>{account.status === 'active' && <button type="button" onClick={() => onCloseAccount(account.id)}>Закрыть</button>}</div>
          </article>
        ))}
      </div>
      <section className="card"><div className="section-head"><h2>Действия</h2></div><div className="filter-tabs"><button onClick={() => onAction('topup')}>Пополнить</button><button onClick={() => onAction('pay')}>Списать</button><button onClick={() => onAction('transfer')}>Перевести</button><button onClick={() => onAction('exchange')}>Обменять</button></div></section>
    </div>
  );
}

export default AccountsPage;
