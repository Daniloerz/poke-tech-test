package com.poketechtest.application.port.out;

import com.poketechtest.domain.model.User;

public interface TokenIssuer {

    IssuedToken issue(User user);
}
