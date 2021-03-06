
package org.nzbhydra.mapping.newznab.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "@attributes"
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewznabJsonEnclosure {

    @JsonProperty("@attributes")
    public NewznabJsonEnclosureAttributes attributes;

}
