package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.exception.ResourceNotFoundException;
import com.example.eCommerceUdemy.model.Address;
import com.example.eCommerceUdemy.model.User;
import com.example.eCommerceUdemy.payload.AddressDTO;
import com.example.eCommerceUdemy.repository.AddressRepository;
import com.example.eCommerceUdemy.repository.UserRepository;
import com.example.eCommerceUdemy.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    AddressRepository addressRepository;
    @Autowired
    private AuthUtil authUtil;
    @Autowired
    private UserRepository userRepository;

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {
        Address address = modelMapper.map(addressDTO, Address.class);

        List<Address> addressList = user.getAddresses();
        addressList.add(address);
        user.setAddresses(addressList);

        address.setUser(user);
        Address savedAddress = addressRepository.save(address);
        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getAddresses() {
        List<Address> addressList = addressRepository.findAll();
        List<AddressDTO> addressDTOList = addressList.stream().map(address ->
                modelMapper.map(address, AddressDTO.class))
                .collect(Collectors.toList());
        return addressDTOList;
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
        AddressDTO addressDTO = modelMapper.map(address, AddressDTO.class);
        return addressDTO;
    }

    @Override
    public List<AddressDTO> getUserAddresses(User user) {
        List<Address> addressList = user.getAddresses();
        List<AddressDTO> addressDTOList = addressList.stream().map(address ->
                        modelMapper.map(address, AddressDTO.class))
                .collect(Collectors.toList());
        return addressDTOList;
    }

    @Override
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        Address addressFromDB = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        addressFromDB.setCity(addressDTO.getCity());
        addressFromDB.setState(addressDTO.getState());
        addressFromDB.setStreet(addressDTO.getStreet());
        addressFromDB.setPincode(addressDTO.getPincode());
        addressFromDB.setCountry(addressDTO.getCountry());
        addressFromDB.setBuildingName(addressDTO.getBuildingName());
        Address updatedAddress = addressRepository.save(addressFromDB);

//      update address for one user
        User user = updatedAddress.getUser();
        user.getAddresses().removeIf(address -> address.getAddressId().equals(addressId));
        user.getAddresses().add(updatedAddress);
        userRepository.save(user);

        return modelMapper.map(updatedAddress, AddressDTO.class);
    }

    @Transactional
    @Override
    public String deletedAddress(Long addressId) {
        Address addressFromDB = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        //update address for one user
        User user = addressFromDB.getUser();
        user.getAddresses().removeIf(address -> address.getAddressId().equals(addressId));
        userRepository.save(user);

        addressRepository.delete(addressFromDB);

        return "Address removed with id: " + addressId;
    }


}
