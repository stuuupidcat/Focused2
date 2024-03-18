package model;

import ai.serenade.treesitter.Node;
import ai.serenade.treesitter.Tree;
import ai.serenade.treesitter.TreeCursor;
import org.treesitter.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class CSTTree implements Serializable {
  public String nodeType;
  public String filePath;
  public String snippet;
  public String fieldName;
  public int startIdx, endIdx;
  public Map<String, List<CSTTree>> children; // key: type, value: CSTTrees
  public List<CSTTree> childrenSeq;
  public Map<String, String> fields;
  public CSTTree parent;

  // init from node
  public CSTTree(
      String nodeType, String filePath, String snippet, CSTTree parent, int startIdx, int endIdx) {
    this.nodeType = nodeType;
    this.filePath = filePath;
    this.snippet = snippet;
    this.startIdx = startIdx;
    this.endIdx = endIdx;
    this.parent = parent;
    children = new HashMap<>();
    childrenSeq = new ArrayList<>();
    fields = new HashMap<>();
  }

  // init from tree
  public CSTTree(String filePath, String source, Tree tree) {
    this(filePath, source, null, tree.getRootNode().walk(), null);
  }

  public CSTTree(String filePath, String source, TSTree tree, TSLanguage language) {
    this(filePath, source, null, tree.getRootNode(), null, language);
  }

  // init from cursor
  public CSTTree(
      String filePath, String source, CSTTree parent, TreeCursor cursor, String fieldName) {
    if (cursor == null) return;
    this.filePath = filePath;
    this.nodeType = removePreUnderscore(cursor.getCurrentNode().getType());
    this.fieldName = fieldName;
    this.startIdx = cursor.getCurrentNode().getStartByte();
    this.endIdx = cursor.getCurrentNode().getEndByte();
    this.snippet = source.substring(startIdx, endIdx);
    this.children = new HashMap<>();
    this.childrenSeq = new ArrayList<>();
    this.fields = new HashMap<>();
    this.parent = parent;

    if (cursor.gotoFirstChild()) {
      Node cur = cursor.getCurrentNode();
      addChild(new CSTTree(filePath, source, this, cur.walk(), cursor.getCurrentFieldName()));
      while (cursor.gotoNextSibling()) {
        cur = cursor.getCurrentNode();
        addChild(new CSTTree(filePath, source, this, cur.walk(), cursor.getCurrentFieldName()));
      }
    }
  }

  private int coorToIdx(TSPoint point, String source) {
    int row = point.getRow(), col = point.getColumn();
    int idx = 0;
    String[] lines = source.split("\n");
    for (int i = 0; i < row; i++) {
      idx += (lines[i] + "\n").getBytes().length;
    }
    idx += col;
    return idx;
  }

  private int byteIndexToCharIndex(int byteIndex, String source) {
    byte[] bytes = Arrays.copyOfRange(source.getBytes(), 0, byteIndex);
    return new String(bytes).length();
  }

  public CSTTree(
      String filePath,
      String source,
      CSTTree parent,
      TSNode cur,
      String fieldName,
      TSLanguage language) {
    if (cur == null) return;
    this.filePath = filePath;
    this.nodeType = removePreUnderscore(cur.getType());
    this.fieldName = fieldName;
    this.startIdx = byteIndexToCharIndex(coorToIdx(cur.getStartPoint(), source), source);
    this.endIdx = byteIndexToCharIndex(coorToIdx(cur.getEndPoint(), source), source);
//    byte[] bytes = Arrays.copyOfRange(source.getBytes(), startIdx, endIdx);
//    this.snippet = new String(bytes);
    this.snippet = source.substring(startIdx, endIdx);
    this.children = new HashMap<>();
    this.childrenSeq = new ArrayList<>();
    this.fields = new HashMap<>();
    this.parent = parent;

    for (int idx = 0; idx < cur.getChildCount(); idx++) {
      TSNode namedNode = cur.getChild(idx);
      String childFieldName = cur.getFieldNameForChild(idx);
//      if ((childFieldName == null || childFieldName.equals("")) && !namedNode.isNamed()) {
//        continue;
//      }

      addChild(new CSTTree(filePath, source, this, namedNode, fieldName, language));
    }
  }

  public List<CSTTree> getDescendantsByType(String source) {
    int pointIdx = source.indexOf(".");
    if (pointIdx == -1) pointIdx = source.length();
    String beforePoint = source.substring(0, pointIdx),
        afterPoint = pointIdx == source.length() ? "" : source.substring(pointIdx + 1);
    List<CSTTree> res = new ArrayList<>();
    beforePoint = beforePoint.replaceAll("\\*", ".+");
    String finalBeforePoint = beforePoint;
    List<String> childrenKey =
        children.keySet().stream()
            .filter(childKey -> childKey.matches(finalBeforePoint))
            .collect(Collectors.toList());

    if (afterPoint.isBlank()) {
      childrenKey.forEach(key -> res.addAll(children.get(key)));
    } else {
      childrenKey.forEach(
          key ->
              children
                  .get(key)
                  .forEach(child -> res.addAll(child.getDescendantsByType(afterPoint))));
    }
    return res;
  }

  public String getDescendantByField(String source) {
    return fields.get(source);
  }

  private void addChild(CSTTree child) {
    String childType = child.nodeType;
    if (!children.containsKey(childType)) {
      children.put(childType, new ArrayList<>());
    }
    children.get(childType).add(child);
    childrenSeq.add(child);

    if (child.fieldName != null) {
      fields.put(child.fieldName, child.snippet);
    }
  }

  private String removePreUnderscore(String type) {
    while (type.startsWith("_")) {
      type = type.substring(1);
    }
    return type;
  }
}
