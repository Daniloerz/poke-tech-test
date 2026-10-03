package com.poketechtest.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.poketechtest.application.exception.ExternalServiceException;
import com.poketechtest.application.exception.PokemonNotFoundException;
import com.poketechtest.application.port.out.PokemonCatalogPort;
import com.poketechtest.domain.model.EvolutionNode;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.domain.model.PokemonDetail;
import com.poketechtest.domain.model.PokemonSpecies;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPokemonDetailUseCaseTest {

    private static final int EEVEE_CHAIN_ID = 67;

    @Mock
    private PokemonCatalogPort pokemonCatalogPort;

    private GetPokemonDetailUseCase getPokemonDetailUseCase;

    @BeforeEach
    void setUp() {
        getPokemonDetailUseCase = new GetPokemonDetailUseCase(pokemonCatalogPort);
    }

    @Test
    void buildsTheDetailFromPokemonSpeciesAndEvolutionChain() {
        EvolutionNode chain = new EvolutionNode(133, "eevee", "133.png", List.of(new EvolutionNode(134, "vaporeon", "134.png", List.of())));
        when(pokemonCatalogPort.findByIdOrName("eevee")).thenReturn(Optional.of(eevee()));
        when(pokemonCatalogPort.findSpecies("eevee")).thenReturn(Optional.of(new PokemonSpecies("eevee", "Evolves in many ways.", EEVEE_CHAIN_ID)));
        when(pokemonCatalogPort.findEvolutionChain(EEVEE_CHAIN_ID)).thenReturn(Optional.of(chain));

        PokemonDetail detail = getPokemonDetailUseCase.get("eevee");

        assertThat(detail.pokemon().id()).isEqualTo(133);
        assertThat(detail.description()).isEqualTo("Evolves in many ways.");
        assertThat(detail.evolutionChain()).isEqualTo(chain);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Eevee", "EEVEE", "  eevee  "})
    void normalizesTheIdentifier(String identifier) {
        when(pokemonCatalogPort.findByIdOrName("eevee")).thenReturn(Optional.of(eevee()));
        when(pokemonCatalogPort.findSpecies("eevee")).thenReturn(Optional.of(new PokemonSpecies("eevee", null, null)));

        PokemonDetail detail = getPokemonDetailUseCase.get(identifier);

        assertThat(detail.pokemon().name()).isEqualTo("eevee");
    }

    @Test
    void acceptsTheNumericId() {
        when(pokemonCatalogPort.findByIdOrName("133")).thenReturn(Optional.of(eevee()));
        when(pokemonCatalogPort.findSpecies("eevee")).thenReturn(Optional.of(new PokemonSpecies("eevee", null, null)));

        assertThat(getPokemonDetailUseCase.get("133").pokemon().name()).isEqualTo("eevee");
    }

    @Test
    void usesTheSpeciesNameNotTheFormName() {
        Pokemon deoxysAttack = Pokemon.builder().id(10001).name("deoxys-attack").speciesName("deoxys").build();
        when(pokemonCatalogPort.findByIdOrName("deoxys-attack")).thenReturn(Optional.of(deoxysAttack));
        when(pokemonCatalogPort.findSpecies("deoxys")).thenReturn(Optional.of(new PokemonSpecies("deoxys", "DNA mutated.", null)));

        assertThat(getPokemonDetailUseCase.get("deoxys-attack").description()).isEqualTo("DNA mutated.");
    }

    @Test
    void throwsNotFoundWhenThePokemonDoesNotExist() {
        when(pokemonCatalogPort.findByIdOrName("missingno")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getPokemonDetailUseCase.get("missingno"))
                .isInstanceOf(PokemonNotFoundException.class)
                .hasMessage("Pokemon not found: missingno");
        verify(pokemonCatalogPort, never()).findSpecies(anyString());
    }

    @Test
    void returnsNoChainWhenTheSpeciesHasNone() {
        when(pokemonCatalogPort.findByIdOrName("eevee")).thenReturn(Optional.of(eevee()));
        when(pokemonCatalogPort.findSpecies("eevee")).thenReturn(Optional.of(new PokemonSpecies("eevee", "text", null)));

        PokemonDetail detail = getPokemonDetailUseCase.get("eevee");

        assertThat(detail.evolutionChain()).isNull();
        verify(pokemonCatalogPort, never()).findEvolutionChain(anyInt());
    }

    @Test
    void returnsNoChainWhenTheChainIsNotFound() {
        when(pokemonCatalogPort.findByIdOrName("eevee")).thenReturn(Optional.of(eevee()));
        when(pokemonCatalogPort.findSpecies("eevee")).thenReturn(Optional.of(new PokemonSpecies("eevee", "text", EEVEE_CHAIN_ID)));
        when(pokemonCatalogPort.findEvolutionChain(EEVEE_CHAIN_ID)).thenReturn(Optional.empty());

        assertThat(getPokemonDetailUseCase.get("eevee").evolutionChain()).isNull();
    }

    @Test
    void failsWhenTheSpeciesOfAnExistingPokemonIsMissing() {
        when(pokemonCatalogPort.findByIdOrName("eevee")).thenReturn(Optional.of(eevee()));
        when(pokemonCatalogPort.findSpecies("eevee")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getPokemonDetailUseCase.get("eevee")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void failsWhenThePokemonHasNoSpeciesName() {
        Pokemon withoutSpecies = Pokemon.builder().id(133).name("eevee").build();
        when(pokemonCatalogPort.findByIdOrName("eevee")).thenReturn(Optional.of(withoutSpecies));

        assertThatThrownBy(() -> getPokemonDetailUseCase.get("eevee")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void propagatesPokeApiErrors() {
        when(pokemonCatalogPort.findByIdOrName("eevee")).thenThrow(new ExternalServiceException("PokeAPI is not available"));

        assertThatThrownBy(() -> getPokemonDetailUseCase.get("eevee")).isInstanceOf(ExternalServiceException.class);
    }

    private Pokemon eevee() {
        return Pokemon.builder().id(133).name("eevee").speciesName("eevee").build();
    }
}
