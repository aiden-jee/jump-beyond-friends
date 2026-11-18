
package com.aidenjee.jumpbeyondfriends.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    private static final String FRONTEND_PATH =
            "{first:(?!(?:api|oauth2|assets|error|actuator)$)[a-zA-Z0-9_-]+}";

    @GetMapping({
        "/" + FRONTEND_PATH,
        "/" + FRONTEND_PATH + "/{*remaining}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
