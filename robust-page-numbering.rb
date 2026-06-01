require 'asciidoctor/pdf'

# Roman front matter up to the first real chapter; arabic from it.
#
# asciidoctor-pdf natively treats only the title page + TOC as front matter and numbers
# everything else (preamble, every section) as the arabic body. This curriculum needs the
# copyright, the TOC and the learning-goals overview to count as roman front matter, with
# arabic page 1 starting at the first real chapter (e.g. "Einfuehrung"/"Introduction").
#
# The treeprocessor tags that chapter with the role "arabic-start". Here we read the
# physical page on which it begins and use (page - 1) as the front-matter page count that
# drives the roman/arabic boundary. Everything is derived at render time, so it is robust
# regardless of how many pages the copyright / overview / TOC span.
module ISAQB
  module RomanFrontMatter
    def convert_section sect, opts = {}
      result = super
      if @isaqb_front_matter_pages.nil? && (sect.role? 'arabic-start')
        # pdf-page-start is set by the base convert_section after the chapter page break.
        @isaqb_front_matter_pages = (sect.attr 'pdf-page-start').to_i - 1
      end
      result
    end

    def isaqb_front_matter_pages(fallback)
      @isaqb_front_matter_pages || fallback
    end

    # Footer page numbers + the roman/arabic switch.
    def ink_running_content(periphery, doc, skip = [1, 1], body_start_page_number = 1)
      skip = [skip[0], isaqb_front_matter_pages(skip[1])]
      super periphery, doc, skip, body_start_page_number
    end

    # Page numbers shown inside the TOC.
    def ink_toc(doc, num_levels, toc_page_number, start_cursor, num_front_matter_pages = 0)
      num_front_matter_pages = isaqb_front_matter_pages(num_front_matter_pages)
      super
    end

    # Page labels in the PDF outline (bookmarks).
    def add_outline(doc, num_levels, toc_page_nums, num_front_matter_pages, has_front_cover)
      num_front_matter_pages = isaqb_front_matter_pages(num_front_matter_pages)
      super
    end
  end
end

Asciidoctor::PDF::Converter.prepend ISAQB::RomanFrontMatter
