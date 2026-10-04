import { apiRequest } from './client.js';

export function fetchPokemonPage(page, size) {
  return apiRequest(`/pokemon?page=${page}&size=${size}`);
}

export function fetchPokemonDetail(idOrName) {
  return apiRequest(`/pokemon/${encodeURIComponent(idOrName)}`);
}
