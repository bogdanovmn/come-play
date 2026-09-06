package com.github.bogdanovmn.comeplay.sport;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class SportTypeService {

    private final SportTypeRepository sportTypeRepository;

    @Transactional(readOnly = true)
    public List<SportType> list() {
        return sportTypeRepository.findAll();
    }

    @Transactional
    public SportType add(String name) {
        if (sportTypeRepository.findAll().stream().anyMatch(s -> s.getName().equalsIgnoreCase(name))) {
            throw new IllegalArgumentException("Sport type already exists: " + name);
        }
        int id = sportTypeRepository.insert(name);
        return SportType.builder().id(id).name(name).build();
    }

    @Transactional(readOnly = true)
    public SportType requireById(int id) {
        return sportTypeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Sport type not found: " + id));
    }
}