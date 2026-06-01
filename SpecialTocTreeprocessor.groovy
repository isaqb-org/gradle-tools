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

    def blocks = document.getBlocks()

    def sectionBlock = createSection(document, 1, false, [:])
    sectionBlock.setTitle(sectionTitle)
    // treeprocessor-created sections skip asciidoctor's auto ID generation; set one
    // explicitly so the TOC/xref links resolve instead of jumping to the document top.
    sectionBlock.setId("learning-goals-overview")
    def listBlock = createList(sectionBlock, "ulist")
    sectionBlock.getBlocks().add(listBlock)

    def topSections = blocks.findAll { it.getNodeName() == "section" }
    def copyrightSection = topSections.find { it.getId() == "copyright" } ?: topSections[0]

    // Tag the first real chapter so robust-page-numbering.rb knows where arabic page 1 starts.
    def firstChapter = topSections.find { it != copyrightSection }
    firstChapter?.addRole("arabic-start")

    // Insert the overview after the copyright section, which renders after the toc::[]
    // (authored as copyright's last child) -> order: copyright, TOC, overview, chapters.
    if (copyrightSection != null) {
        blocks.add(blocks.indexOf(copyrightSection) + 1, sectionBlock)
    } else {
        def idx = firstChapter != null ? blocks.indexOf(firstChapter) : 0
        blocks.add(idx, sectionBlock)
    }

    // Add each learning goal as a list item with proper xref
    learningGoalSections.each { section ->
        def title = section.getTitle()
        def id = section.getId()

        def cleanTitle = title.replaceAll(/<sup>(.*?)<\/sup>/, '^$1^')
        cleanTitle = cleanTitle.replaceAll(/<sub>(.*?)<\/sub>/, '~$1~')
        cleanTitle = unescapeHtmlEntities(cleanTitle)
        cleanTitle = escapeForXref(cleanTitle);

        // Create a list item
        def listItem = createListItem(listBlock, "xref:${id}[${cleanTitle}]")
        listBlock.getBlocks().add(listItem)
    }

    return document
}

static def unescapeHtmlEntities(String input) {
    return input
            .replace('&amp;', '&')
            .replace('&lt;', '<')
            .replace('&gt;', '>')
            .replace('&quot;', '"')
            .replace('&#39;', "'")
}

static def escapeForXref(String input) {
    return input
            .replace('[', '&#91;')
            .replace(']', '&#93;')
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
