package top.itangbao.platform.workflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.itangbao.platform.workflow.entity.FormDefinition;
import java.util.Optional;

@Repository
public interface FormDefinitionRepository extends JpaRepository<FormDefinition, Long> {
    Optional<FormDefinition> findByFormKey(String formKey);
}
