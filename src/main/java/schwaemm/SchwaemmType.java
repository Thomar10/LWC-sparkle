package schwaemm;

/**
 * schwaemm.Schwaemm type and helper functions for the type.
 */
public enum SchwaemmType {
  S128128("128128"), S192192("192192"), S256128("256128"), S256256("256256");

  private final String type;

  SchwaemmType(String type) {
    this.type = type;
  }

  public int getTagBytes() {
    return Integer.parseInt(this.type.substring(3, 6)) / 8;
  }

  public int getStateSize() {
    switch (this.type) {
      case "128128" -> {
        return 8;
      }
      case "192192", "256128" -> {
        return 12;
      }
      case "256256" -> {
        return 16;
      }
      default -> {
        return -1;
      }
    }
  }

  public int getCipherLength() {
    if ("256128".equals(this.type)) {
      return this.getStateSize() / 2 + 2;
    } else {
      return this.getStateSize() / 2;
    }
  }

  public int getVerifyTagLength() {
    if ("256128".equals(this.type)) {
      return 4;
    }
    return this.getStateSize() / 2;
  }

  public String getType() {
    return type;
  }

  public int getNonceSize() {
    switch (this.type) {
      case "128128" -> {
        return 16;
      }
      case "192192" -> {
        return 24;
      }
      case "256128", "256256" -> {
        return 32;
      }
      default -> {
        return -1;
      }
    }
  }

  public int getKeySize() {
    switch (this.type) {
      case "128128", "256128" -> {
        return 16;
      }
      case "192192" -> {
        return 24;
      }
      case "256256" -> {
        return 32;
      }
      default -> {
        return -1;
      }
    }
  }
}
