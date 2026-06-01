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

    // The learning-goals overview must stay in the roman-numbered front matter, i.e.
    // BEFORE the TOC (page numbering uses "start_at: after-toc"). We therefore attach it
    // to the preamble as a discrete (floating) title + list, inserted just before the TOC
    // block, instead of as a top-level chapter. As a discrete title it is intentionally
    // not numbered and not listed in the TOC.
    def blocks = document.getBlocks()
    def preamble = blocks.find { it.getNodeName() == "preamble" }

    def listBlock
    if (preamble) {
        def heading = createBlock(preamble, "floating_title", [:])
        heading.setLevel(1)
        heading.setTitle(sectionTitle)

        listBlock = createList(preamble, "ulist")

        // Insert before the TOC block so the overview stays ahead of the TOC (PDF places
        // the TOC at the end of the front matter via :toc: macro). If there is no TOC
        // block in the preamble (e.g. HTML, where the TOC is a left sidebar), append.
        def preBlocks = preamble.getBlocks()
        def tocIndex = preBlocks.findIndexOf {
            it.getNodeName() == "toc" || it.getContext()?.toString() == "toc"
        }
        if (tocIndex >= 0) {
            preBlocks.add(tocIndex, listBlock)
            preBlocks.add(tocIndex, heading)
        } else {
            preBlocks.add(heading)
            preBlocks.add(listBlock)
        }
    } else {
        // Fallback (no preamble): insert a real top-level section before the first chapter.
        // This variant IS numbered and appears in the TOC.
        def insertIndex = 0
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks[i].getNodeName() == "section") {
                insertIndex = i
                break
            }
        }

        def sectionBlock = createSection(document, 1, false, [:])
        sectionBlock.setTitle(sectionTitle)
        listBlock = createList(sectionBlock, "ulist")
        sectionBlock.getBlocks().add(listBlock)
        blocks.add(insertIndex, sectionBlock)
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
