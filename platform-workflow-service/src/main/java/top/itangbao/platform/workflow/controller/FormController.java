package top.itangbao.platform.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.workflow.entity.FormDefinition;
import top.itangbao.platform.workflow.repository.FormDefinitionRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow/forms")
public class FormController {

    @Autowired
    private FormDefinitionRepository formDefinitionRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/definitions")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')")
    public ResponseEntity<List<FormDefinition>> getFormDefinitions() {
        return ResponseEntity.ok(formDefinitionRepository.findAll());
    }

    @GetMapping("/definitions/{key}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_USER')")
    public ResponseEntity<FormDefinition> getFormDefinition(@PathVariable String key) {
        return formDefinitionRepository.findByFormKey(key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/definitions")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
    public ResponseEntity<FormDefinition> createFormDefinition(@RequestBody Map<String, Object> request) {
        try {
            FormDefinition form = new FormDefinition();
            form.setFormKey((String) request.get("key"));
            form.setName((String) request.get("name"));
            form.setSchema(objectMapper.writeValueAsString(request.get("schema")));
            form.setTenantId("default");
            return new ResponseEntity<>(formDefinitionRepository.save(form), HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/definitions/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TENANT_ADMIN')")
    public ResponseEntity<FormDefinition> updateFormDefinition(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return (ResponseEntity<FormDefinition>) formDefinitionRepository.findById(id)
                .map(form -> {
                    try {
                        if (request.containsKey("name")) form.setName((String) request.get("name"));
                        if (request.containsKey("schema")) form.setSchema(objectMapper.writeValueAsString(request.get("schema")));
                        return ResponseEntity.ok(formDefinitionRepository.save(form));
                    } catch (Exception e) {
                        return ResponseEntity.badRequest().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
