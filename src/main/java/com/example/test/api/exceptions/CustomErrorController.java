package com.example.test.api.exceptions;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.coyote.Response;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Controller
public class CustomErrorController implements ErrorController {
    private static final String PATH = "/error";

    ErrorAttributes errorAttributes;

    @RequestMapping(CustomErrorController.PATH)
    public ResponseEntity<ErrorDTO> error(WebRequest webRequest) {

        Map<String, Object> attributes = errorAttributes.getErrorAttributes(
                webRequest,
                ErrorAttributeOptions.of(ErrorAttributeOptions.Include.EXCEPTION, ErrorAttributeOptions.Include.MESSAGE)
        );
        Object statusObject = attributes.get("status");
        int status = (statusObject instanceof Integer) ? (Integer) statusObject : HttpStatus.INTERNAL_SERVER_ERROR.value();
        return ResponseEntity
                .status(status)
                .body(ErrorDTO
                        .builder()
                        .error((String) attributes.get("error"))
                        .errorDescription((String) attributes.get("message"))
                        .build()
                );

    }

}
