package model;

import java.util.Arrays;

public enum Language {
  C(".c", "C"),
  CPP(".cpp", "CPP"),
  HPP(".h", "HPP"),
  Python(".py", "Python"),
  JAVA(".java", "JAVA"),
  XML(".xml", "XML"),
  HTML(".html", "HTML"),
  JSP(".jsp", "JSP"),
  FTL(".ftl", "FTL"),
  SQL(".sql", "SQL"),
  CSS(".css", "CSS"),
  FILE(".file", "FILE"),
  ANY("*", "ANY"),
  OTHER("?", "?");

  public String extension;
  public String identifier;

  Language(String extension, String identifier) {
    this.extension = extension;
    this.identifier = identifier;
  }

  public static Language valueOfLabel(String s) {
    return Arrays.stream(Language.values())
        .filter(
            l ->
                (l.extension.equals(s) || ("*" + l.extension).equals(s) || l.extension.endsWith(s)))
        .findFirst()
        .orElse(Language.FILE);
  }

  public String getIdentifier() {
    return identifier;
  }
}
