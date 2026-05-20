import type { Account, CurrencyCode, CurrencyRate, Transaction } from '../types/banking';

export function toRub(amount: number, currency: CurrencyCode, rates: CurrencyRate[]) {
  if (currency === 'RUB') return amount;
  const rate = rates.find((item) => item.code === currency);
  return rate ? amount * rate.sell : amount;
}

export function getTotalBalance(accounts: Account[], rates: CurrencyRate[]) {
  return accounts.filter((account) => account.status === 'active').reduce((sum, account) => sum + toRub(account.balance, account.currency, rates), 0);
}
export function getMonthlyIncome(transactions: Transaction[]) { return transactions.filter((tx) => tx.amount > 0 && tx.status !== 'failed').reduce((sum, tx) => sum + tx.amount, 0); }
export function getMonthlyOutcome(transactions: Transaction[]) { return Math.abs(transactions.filter((tx) => tx.amount < 0 && tx.status !== 'failed').reduce((sum, tx) => sum + tx.amount, 0)); }
export function getCategoryTotals(transactions: Transaction[]) { return transactions.filter((tx) => tx.amount < 0 && tx.status !== 'failed').reduce<Record<string, number>>((acc, tx) => { acc[tx.category] = (acc[tx.category] ?? 0) + Math.abs(tx.amount); return acc; }, {}); }
export function getStatusText(status: Transaction['status']) { if (status === 'success') return 'Выполнено'; if (status === 'pending') return 'В обработке'; return 'Ошибка'; }
export function createAccountNumber() { const suffix = Array.from({ length: 16 }, () => Math.floor(Math.random() * 10)).join(''); return `4081${suffix}`; }
