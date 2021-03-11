/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.additionalinfo.backend.ws.config;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class WSDevConfig extends WSBaseConfig {

    @Value("${ws.ecdsa.credentials.privateKey:}")
    private String privateKey;

    @Value("${ws.ecdsa.credentials.publicKey:}")
    public String publicKey;

    @Override
    String getPrivateKey() {
        return new String(Base64.getDecoder().decode(privateKey));
    }

    @Override
    String getPublicKey() {
        return new String(Base64.getDecoder().decode(publicKey));
    }

    @Override
    String getSplunkUsername() {
        throw new IllegalStateException("No splunk configuration in dev profile");
    }

    @Override
    String getSplunkpassword() {
        throw new IllegalStateException("No splunk configuration in dev profile");
    }
}
