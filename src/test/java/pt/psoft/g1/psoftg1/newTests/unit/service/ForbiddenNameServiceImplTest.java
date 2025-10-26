package pt.psoft.g1.psoftg1.newTests.unit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.services.ForbiddenNameServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests (opaque-box) for ForbiddenNameServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class ForbiddenNameServiceImplTest {

  @Mock ForbiddenNameRepository repo;
  @Mock IdGenerator idGenerator;

  ForbiddenNameServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new ForbiddenNameServiceImpl(repo, idGenerator);
  }

  @Test
  void loadDataFromFile_savesOnlyMissingNames() {
    when(repo.findByForbiddenName("apple"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(new ForbiddenName("X", "apple")));
    when(repo.findByForbiddenName("banana"))
        .thenReturn(Optional.of(new ForbiddenName("Y", "banana")));

    when(idGenerator.newId()).thenReturn("ID-1");

    service.loadDataFromFile("forbidden-names-test.txt");

    ArgumentCaptor<ForbiddenName> cap = ArgumentCaptor.forClass(ForbiddenName.class);
    verify(repo, times(1)).save(cap.capture());

    ForbiddenName saved = cap.getValue();
    assertEquals("ID-1", saved.getId());
    assertEquals("apple", saved.getForbiddenName());

    verify(repo, times(2)).findByForbiddenName("apple");
    verify(repo, times(1)).findByForbiddenName("banana");
  }

  @Test
  void loadDataFromFile_whenMissing_throwsRuntimeException() {
    assertThrows(RuntimeException.class, () -> service.loadDataFromFile("does-not-exist.txt"));
    verify(repo, never()).save(any());
  }
}
