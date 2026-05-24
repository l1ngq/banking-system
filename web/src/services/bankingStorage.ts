import { createBankingStateForEmail } from '../data/mockData';
import type { BankingState } from '../types/banking';
import { isSupportedBackendCurrency } from './bankingHelpers';

const STORAGE_KEY = 'mik-bank-state-clean-v1';

export function storageKeyForEmail(email?: string | null) {
  const normalizedEmail = email?.trim().toLowerCase();
  return normalizedEmail ? `${STORAGE_KEY}:${normalizedEmail}` : STORAGE_KEY;
}

export function readBankingState(email?: string | null): BankingState {
  const fallbackState = createBankingStateForEmail(email);

  try {
    const saved = localStorage.getItem(storageKeyForEmail(email));
    if (!saved) return fallbackState;

    const parsed = JSON.parse(saved) as BankingState;

    return {
      ...fallbackState,
      ...parsed,
      profile: {
        ...fallbackState.profile,
        ...parsed.profile,
        email: email ?? parsed.profile?.email ?? fallbackState.profile.email,
      },
      rates: (parsed.rates ?? fallbackState.rates).filter((rate) => rate.code === 'USD' || rate.code === 'EUR'),
      accounts: (parsed.accounts ?? fallbackState.accounts).filter((account) => isSupportedBackendCurrency(account.currency)),
      cards: [],
      transactions: (parsed.transactions ?? fallbackState.transactions).filter((transaction) => isSupportedBackendCurrency(transaction.currency)),
      news: [],
      assistantMessages: [],
      goals: [],
      cashbackCategories: [],
    };
  } catch {
    return fallbackState;
  }
}

export function saveBankingState(email: string | null, state: BankingState) {
  if (!email) return;
  localStorage.setItem(storageKeyForEmail(email), JSON.stringify(state));
}
