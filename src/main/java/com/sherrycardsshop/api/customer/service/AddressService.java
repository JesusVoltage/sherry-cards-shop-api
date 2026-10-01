package com.sherrycardsshop.api.customer.service;

import java.util.List;

import com.sherrycardsshop.api.auth.repository.UsuarioRepository;
import com.sherrycardsshop.api.common.exception.ApiException;
import com.sherrycardsshop.api.customer.dto.AddressDto;
import com.sherrycardsshop.api.customer.dto.AddressRequest;
import com.sherrycardsshop.api.customer.entity.DireccionUsuario;
import com.sherrycardsshop.api.customer.mapper.AddressMapper;
import com.sherrycardsshop.api.customer.repository.DireccionUsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Direcciones del usuario autenticado. Cada usuario tiene como máximo una dirección
 * predeterminada de envío y otra de facturación: la primera de cada tipo lo es automáticamente
 * y, al borrar o desmarcar la predeterminada, pasa a serlo la más antigua de las demás de ese tipo.
 */
@Service
public class AddressService {

    static final int MAX_ADDRESSES = 20;
    private static final long NO_ADDRESS = 0L;

    private final DireccionUsuarioRepository direccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AddressMapper addressMapper;

    public AddressService(DireccionUsuarioRepository direccionRepository, UsuarioRepository usuarioRepository,
                          AddressMapper addressMapper) {
        this.direccionRepository = direccionRepository;
        this.usuarioRepository = usuarioRepository;
        this.addressMapper = addressMapper;
    }

    @Transactional(readOnly = true)
    public List<AddressDto> getAddresses(Long usuarioId) {
        return addressMapper.toDtoList(direccionRepository.findAllByUsuarioIdOrderByIdAsc(usuarioId));
    }

    @Transactional
    public AddressDto createAddress(Long usuarioId, AddressRequest request) {
        if (direccionRepository.countByUsuarioId(usuarioId) >= MAX_ADDRESSES) {
            throw new ApiException(HttpStatus.CONFLICT, "Has alcanzado el máximo de " + MAX_ADDRESSES + " direcciones");
        }
        boolean defaultEnvio = request.usoEnvio()
                && (request.predeterminadaEnvio() || !direccionRepository.existsByUsuarioIdAndPredeterminadaEnvioTrue(usuarioId));
        boolean defaultFacturacion = request.usoFacturacion()
                && (request.predeterminadaFacturacion() || !direccionRepository.existsByUsuarioIdAndPredeterminadaFacturacionTrue(usuarioId));
        clearOtherDefaults(usuarioId, NO_ADDRESS, defaultEnvio, defaultFacturacion);

        DireccionUsuario direccion = new DireccionUsuario();
        direccion.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        apply(direccion, request, defaultEnvio, defaultFacturacion);
        return addressMapper.toDto(direccionRepository.saveAndFlush(direccion));
    }

    @Transactional
    public AddressDto updateAddress(Long usuarioId, Long addressId, AddressRequest request) {
        DireccionUsuario direccion = findOwned(usuarioId, addressId);
        boolean defaultEnvio = request.usoEnvio() && request.predeterminadaEnvio();
        boolean defaultFacturacion = request.usoFacturacion() && request.predeterminadaFacturacion();
        // Se desmarcan las demás antes de modificar esta para no violar la restricción única.
        clearOtherDefaults(usuarioId, addressId, defaultEnvio, defaultFacturacion);

        boolean wasDefaultEnvio = direccion.isPredeterminadaEnvio();
        boolean wasDefaultFacturacion = direccion.isPredeterminadaFacturacion();
        apply(direccion, request, defaultEnvio, defaultFacturacion);
        direccionRepository.flush();
        promoteReplacementDefaults(usuarioId, addressId, wasDefaultEnvio && !defaultEnvio, wasDefaultFacturacion && !defaultFacturacion);
        return addressMapper.toDto(direccion);
    }

    @Transactional
    public void deleteAddress(Long usuarioId, Long addressId) {
        DireccionUsuario direccion = findOwned(usuarioId, addressId);
        boolean wasDefaultEnvio = direccion.isPredeterminadaEnvio();
        boolean wasDefaultFacturacion = direccion.isPredeterminadaFacturacion();
        direccionRepository.delete(direccion);
        direccionRepository.flush();
        promoteReplacementDefaults(usuarioId, addressId, wasDefaultEnvio, wasDefaultFacturacion);
    }

    private DireccionUsuario findOwned(Long usuarioId, Long addressId) {
        return direccionRepository.findByIdAndUsuarioId(addressId, usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dirección no encontrada"));
    }

    private void clearOtherDefaults(Long usuarioId, Long exceptId, boolean envio, boolean facturacion) {
        if (envio) {
            direccionRepository.clearDefaultEnvio(usuarioId, exceptId);
        }
        if (facturacion) {
            direccionRepository.clearDefaultFacturacion(usuarioId, exceptId);
        }
    }

    // Si se ha quitado la predeterminada de un tipo, otra dirección de ese tipo toma el relevo
    // para que el checkout tenga siempre una opción por defecto mientras exista alguna.
    private void promoteReplacementDefaults(Long usuarioId, Long exceptId, boolean envio, boolean facturacion) {
        if (envio && !direccionRepository.existsByUsuarioIdAndPredeterminadaEnvioTrue(usuarioId)) {
            direccionRepository.findFirstByUsuarioIdAndUsoEnvioTrueAndIdNotOrderByIdAsc(usuarioId, exceptId)
                    .ifPresent(direccion -> direccion.setPredeterminadaEnvio(true));
        }
        if (facturacion && !direccionRepository.existsByUsuarioIdAndPredeterminadaFacturacionTrue(usuarioId)) {
            direccionRepository.findFirstByUsuarioIdAndUsoFacturacionTrueAndIdNotOrderByIdAsc(usuarioId, exceptId)
                    .ifPresent(direccion -> direccion.setPredeterminadaFacturacion(true));
        }
    }

    private static void apply(DireccionUsuario direccion, AddressRequest request,
                              boolean defaultEnvio, boolean defaultFacturacion) {
        direccion.setAlias(blankToNull(request.alias()));
        direccion.setNombreDestinatario(request.nombreDestinatario().trim());
        direccion.setApellidosDestinatario(request.apellidosDestinatario().trim());
        direccion.setTelefono(blankToNull(request.telefono()));
        direccion.setCalle(request.calle().trim());
        direccion.setNumero(request.numero().trim());
        direccion.setComplemento(blankToNull(request.complemento()));
        direccion.setCodigoPostal(request.codigoPostal().trim());
        direccion.setLocalidad(request.localidad().trim());
        direccion.setProvincia(request.provincia().trim());
        direccion.setPais(request.pais().trim());
        direccion.setUsoEnvio(request.usoEnvio());
        direccion.setUsoFacturacion(request.usoFacturacion());
        direccion.setPredeterminadaEnvio(defaultEnvio);
        direccion.setPredeterminadaFacturacion(defaultFacturacion);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
