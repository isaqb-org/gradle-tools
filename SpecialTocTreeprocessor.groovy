// Create a specialized table of contents for learning goals
// This extension will add a TOC for learning goals right after the main TOC

// Use a treeprocessor to insert our learning goals TOC
treeprocessor { document ->

    // Get document language
    def language = document.getAttribute("language")

    // Determine which learning goal prefix to look for based on language
    def sectionTitle = (language == "DE") ? "Lernziele im Überblick" : "Learning Goals Overview"

    // Collect all matching learning goal sections
    def learningGoalSections = []

    // Process document blocks recursively to find learning goal sections
    document.getBlocks().each { block -> 
        learningGoalSections.addAll(findLearningGoals(block))
    }

    if (learningGoalSections.isEmpty()) {
        return document
    }

    // Sort the learning goals by ID for consistency
    learningGoalSections.sort { a, b -> a.getId() <=> b.getId() }

    // Find the insertion point - after the preamble which contains the TOC
    def blocks = document.getBlocks()
    def insertIndex = 0  // Default to beginning

    // Look for preamble (which contains TOC)
    for (int i = 0; i < blocks.size(); i++) {
        def block = blocks[i]
        if (block.getNodeName() == "preamble") {
            insertIndex = i + 1
            break
        }
    }

    // If no preamble found, look for the first section
    if (insertIndex == 0) {
        for (int i = 0; i < blocks.size(); i++) {
            def block = blocks[i]
            if (block.getNodeName() == "section") {
                insertIndex = i
                break
            }
        }
    }

    def sectionBlock = createSection(document, 1, false, [:])
    sectionBlock.setTitle(sectionTitle)

    // Create a list for the learning goals
    def listBlock = createList(sectionBlock, "ulist")
    sectionBlock.getBlocks().add(listBlock)

    // Add each learning goal as a list item with proper xref
    learningGoalSections.each { section ->
        def title = section.getTitle()
        def id = section.getId()

        // Convert HTML superscript tags to AsciiDoc format for PDF compatibility
        def cleanTitle = title.replaceAll(/<sup>(.*?)<\/sup>/, '^$1^')

        // Create a list item
        def listItem = createListItem(listBlock, "xref:${id}[${cleanTitle}]")
        listBlock.getBlocks().add(listItem)
    }

    // Add section to document
    blocks.add(insertIndex, sectionBlock)

    return document
}

// Helper method to recursively find learning goal sections
def findLearningGoals(block) {
    // Only process if block is not null
    if (!block) return []

    def learningGoals = []

    // Check if this is a section with a learning goal ID
    if (block.getNodeName() == "section") {
        def id = block.getId()
        if (id && (id.startsWith("LG") || id.startsWith("LZ"))) {
            learningGoals << block
        }
    }

    // Recursively process child blocks
    if (block.getBlocks()) {
        block.getBlocks().each { childBlock -> 
            learningGoals.addAll(findLearningGoals(childBlock))
        }
    }

    return learningGoals
}
