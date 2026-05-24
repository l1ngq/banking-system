import type { Account, CurrencyCode, Transaction } from '../types/banking';

export function isSupportedBackendCurrency(currency: CurrencyCode) {
  return currency === 'RUB' || currency === 'USD' || currency === 'EUR';
}

export function uniqueTransactions(items: Transaction[]) {
  const map = new Map<string, Transaction>();
  items.forEach((item) => map.set(item.id, item));
  return Array.from(map.values()).sort(
    (left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime(),
  );
}

export function replaceAccount(accounts: Account[], updated: Account) {
  const exists = accounts.some((account) => account.id === updated.id);
  if (!exists) return [updated, ...accounts];
  return accounts.map((account) => (account.id === updated.id ? { ...account, ...updated } : account));
}

export function resolveBackendAccountId(value: string, accounts: Account[]) {
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

export function isNumericAccountId(value: string) {
  return /^\d+$/.test(value.trim());
}

export function isValidExternalAccountNumber(value: string) {
  return /^\d{10,30}$/.test(value.replace(/\D/g, ''));
}
