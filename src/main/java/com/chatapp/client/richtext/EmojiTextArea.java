package com.chatapp.client.richtext;

import javafx.scene.Node;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.StyledSegment;
import org.reactfx.util.Either;

import java.util.function.BiConsumer;

public class EmojiTextArea extends GenericStyledArea<String, Either<String, LinkedImage>, String> {

    public EmojiTextArea() {
        super(
            "", // default paragraph style
            (textFlow, style) -> textFlow.setStyle(style),
            "", // default segment style
            SegmentOps.<String>styledTextOps()._or(
                new LinkedImageOps<String>(),
                (s1, s2) -> java.util.Optional.empty()
            ),
            seg -> createNode(seg, (text, style) -> text.setStyle(style))
        );
        this.setWrapText(true);
        this.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 14px;");
    }

    private static Node createNode(StyledSegment<Either<String, LinkedImage>, String> seg,
                                   BiConsumer<? super org.fxmisc.richtext.TextExt, String> applyStyle) {
        return seg.getSegment().unify(
            text -> StyledTextArea.createStyledTextNode(text, seg.getStyle(), applyStyle),
            LinkedImage::createNode
        );
    }
    
    public void insertEmoji(String imagePath) {
        int caret = this.getCaretPosition();
        org.fxmisc.richtext.model.ReadOnlyStyledDocument<String, Either<String, LinkedImage>, String> doc = 
            org.fxmisc.richtext.model.ReadOnlyStyledDocument.fromSegment(
                Either.right(new RealLinkedImage(imagePath)),
                "", "", this.getSegOps()
            );
        this.replaceSelection(doc);
    }
    
    public String getPlainTextWithEmojis() {
        StringBuilder sb = new StringBuilder();
        for (org.fxmisc.richtext.model.Paragraph<String, Either<String, LinkedImage>, String> p : this.getParagraphs()) {
            for (StyledSegment<Either<String, LinkedImage>, String> seg : p.getStyledSegments()) {
                seg.getSegment().exec(
                    text -> sb.append(text),
                    img -> {
                        if (img.isReal()) {
                            String path = img.getImagePath();
                            String filename = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
                            try {
                                if (filename.equals("2764")) {
                                    sb.append("❤️");
                                } else {
                                    int codePoint = Integer.parseInt(filename, 16);
                                    sb.append(new String(Character.toChars(codePoint)));
                                }
                            } catch (Exception e) {}
                        }
                    }
                );
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }
}
