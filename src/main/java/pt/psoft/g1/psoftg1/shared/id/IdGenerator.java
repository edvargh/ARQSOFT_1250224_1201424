package pt.psoft.g1.psoftg1.shared.id;

public interface IdGenerator {
  String newId();
  String newId(String prefix);
}

