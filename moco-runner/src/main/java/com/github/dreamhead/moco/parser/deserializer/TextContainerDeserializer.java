package com.github.dreamhead.moco.parser.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.github.dreamhead.moco.parser.model.TextContainer;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Maps.transformEntries;

public class TextContainerDeserializer extends JsonDeserializer<TextContainer> {
    @Override
    public TextContainer deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonToken currentToken = jp.getCurrentToken();
        if (currentToken == JsonToken.VALUE_STRING) {
            return text(jp);
        } else if (currentToken == JsonToken.START_OBJECT) {
            jp.nextToken();
            return textContainer(jp, ctxt);
        }

        throw ctxt.mappingException(TextContainer.class, currentToken);
    }

    private TextContainer textContainer(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonToken currentToken = jp.getCurrentToken();
        if (currentToken == JsonToken.FIELD_NAME) {
            TextContainer.Builder builder = TextContainer.builder();
            String operation = jp.getText().trim();
            builder.withOperation(operation);
            JsonToken token = jp.nextToken();
            if (token == JsonToken.VALUE_STRING) {
                String text = jp.getText().trim();
                jp.nextToken();
                return builder.withText(text).build();
            }

            if (TextContainer.isForTemplate(operation) && token == JsonToken.START_OBJECT) {
                Iterator<Template> iterator = jp.readValuesAs(Template.class);
                Template template = Iterators.get(iterator, 0);
                jp.nextToken();
                return builder.withText(template.with).withProps(toTemplateVars(template)).build();
            }
        }

        throw ctxt.mappingException(TextContainer.class, jp.getCurrentToken());
    }

    private ImmutableMap<String, TextContainer> toTemplateVars(Template template) {
        return copyOf(transformEntries(template.vars, toLocalContainer()));
    }

    private Maps.EntryTransformer<String, TextContainer, TextContainer> toLocalContainer() {
        return new Maps.EntryTransformer<String, TextContainer, TextContainer>() {
            @Override
            public TextContainer transformEntry(String key, TextContainer container) {
                if (container.isRawText()) {
                    return container;
                }

                return toLocal(container);
            }
        };
    }

    private final ImmutableMap<String, String> names = ImmutableMap.of("json_paths", "jsonPaths");

    private TextContainer toLocal(TextContainer container) {
        String name = names.get(container.getOperation());
        return name == null ? container : TextContainer.builder().withOperation(name).withText(container.getText()).withProps(container.getProps()).build();
    }

    private static class Template {
        public String with;
        public Map<String, TextContainer> vars;
    }

    private TextContainer text(JsonParser jp) throws IOException {
        return TextContainer.builder().withText(jp.getText().trim()).build();
    }
}
