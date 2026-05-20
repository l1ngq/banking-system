import type { Account, Action, Transaction } from '../types/banking';
import { formatDateTime, formatMoney } from '../utils/formatters';

interface PaymentsPageProps { accounts: Account[]; transactions: Transaction[]; onAction: (action: Action) => void; }

function PaymentsPage({ accounts, transactions, onAction }: PaymentsPageProps) {
  return (
    <div className="page-grid">
      <section className="page-hero"><div><p className="eyebrow">платежи и переводы</p><h2>Операции</h2><p>Выполняйте переводы и просматривайте личную историю операций.</p></div><div className="filter-tabs"><button onClick={() => onAction('transfer')}>Перевод</button><button onClick={() => onAction('pay')}>Платёж</button></div></section>
      <div className="content__row content__row--wide">
        <section className="card"><div className="section-head"><h2>История</h2><button className="text-button" onClick={() => onAction('topup')}>Пополнить</button></div><div className="transactions">{transactions.map((tx) => <div key={tx.id} className="transaction"><div><strong>{tx.title}</strong><span>{tx.category} · {formatDateTime(tx.createdAt)}</span></div><strong className={tx.amount >= 0 ? 'positive' : 'negative'}>{formatMoney(tx.amount, tx.currency)}</strong></div>)}</div></section>
        <section className="card"><h2>Счета</h2><div className="rates-list">{accounts.filter((account) => account.status === 'active').map((account) => <div className="rate-row" key={account.id}><div><strong>{account.name}</strong><span>{account.number.slice(-8)}</span></div><strong>{formatMoney(account.balance, account.currency)}</strong></div>)}</div></section>
      </div>
    </div>
  );
}

export default PaymentsPage;
