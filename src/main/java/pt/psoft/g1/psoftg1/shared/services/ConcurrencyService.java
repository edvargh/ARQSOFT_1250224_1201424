package pt.psoft.g1.psoftg1.shared.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ConcurrencyService {
    public static final String IF_MATCH = "If-Match";

    public long getVersionFromIfMatchHeader(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank() || "null".equalsIgnoreCase(ifMatch)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "You must issue a conditional PATCH using 'If-Match'");
        }
        String v = ifMatch.trim();
        if (v.startsWith("W/")) v = v.substring(2).trim();
        if (v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1);
        }
        return Long.parseLong(v);
    }
}
