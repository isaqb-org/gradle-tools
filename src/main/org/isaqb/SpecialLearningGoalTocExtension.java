package com.example.asciidoctor.extensions;

import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.Treeprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpecialTocTreeprocessor extends Treeprocessor {

  @Override
  public Document process(Document document) {
    // Collect all level 4 sections.
    List<Section> level4Sections = new ArrayList<>();
    collectLearningGoalSections(document.getBlocks(), level4Sections);
    if (level4Sections.isEmpty()) {
      return document;
    }

    // Build the special TOC as AsciiDoc markup.
    List<String> tocLines = new ArrayList<>();
    tocLines.add("== Learning Goals");
    tocLines.add(""); // Blank line for separation.
    for (Section sec : level4Sections) {
      String pageNum = "";
      Object pgAttr = sec.getAttribute("page-number");
      if (pgAttr != null) {
        pageNum = pgAttr.toString();
      }
      String linkText = sec.getTitle();
      if (!pageNum.isEmpty()) {
        linkText += " (" + pageNum + ")";
      }
      tocLines.add("* <<" + sec.getId() + "," + linkText + ">>");
    }

    // Create an open block so that the AsciiDoc markup is processed normally.
    StructuralNode specialTocBlock = createBlock(document, "open", tocLines, new HashMap<>());

    // Insert the special TOC block immediately after the default TOC block if one exists.
    List<StructuralNode> blocks = document.getBlocks();
    int insertIndex = 0;
    for (int i = 0; i < blocks.size(); i++) {
      StructuralNode node = blocks.get(i);
      if ("toc".equals(node.getNodeName())) {
        insertIndex = i + 1;
        break;
      }
    }
    blocks.add(insertIndex, specialTocBlock);

    return document;
  }

  // Helper method to recursively collect all level 4 sections.
  private void collectLearningGoalSections(List<StructuralNode> nodes, List<Section> level4Sections) {
    for (StructuralNode node : nodes) {
      if ("section".equals(node.getNodeName()) && node instanceof Section) {
        Section sec = (Section) node;
        if (sec.getLevel() == 4 && sec.getId() != null && (sec.getId().startsWith("LZ-") || sec.getID().startsWith("LG-"))) {
          learningGoalSections.add(sec);
        }

        collectLearningGoalSections(sec.getBlocks(), level4Sections);
      }
    }
  }
}
