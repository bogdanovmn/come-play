package com.github.bogdanovmn.comeplay.sport;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sport-types")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
class SportTypeController {

    private final SportTypeService sportTypeService;

    @GetMapping
    List<SportType> list() {
        return sportTypeService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('admin')")
    SportType create(@Valid @RequestBody CreateSportTypeRequest request) {
        return sportTypeService.add(request.getName());
    }
}