package br.com.vitor.challengesea.model.address;

/** ViaCEP's answer; field names are ViaCEP's. An unknown CEP comes back without {@code uf}. */
public record ViaCepAddressDTO(String cep, String logradouro, String bairro, String localidade, String uf) {}
