import { apiRequest } from './client.js';

export function fetchLocalPokemonPage(page, size) {
  return apiRequest(`/local-pokemon?page=${page}&size=${size}`);
}

export function fetchLocalPokemon(id) {
  return apiRequest(`/local-pokemon/${id}`);
}

export function syncPokemon(idOrName) {
  return apiRequest('/local-pokemon', { method: 'POST', body: { idOrName } });
}

export function updateLocalPokemon(id, { localizedName, region, tags }) {
  return apiRequest(`/local-pokemon/${id}`, { method: 'PUT', body: { localizedName, region, tags } });
}

export function deleteLocalPokemon(id) {
  return apiRequest(`/local-pokemon/${id}`, { method: 'DELETE' });
}
