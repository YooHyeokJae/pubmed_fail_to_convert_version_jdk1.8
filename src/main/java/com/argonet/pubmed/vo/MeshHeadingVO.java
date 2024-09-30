package com.argonet.pubmed.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MeshHeadingVO {
    private int articleId;
    private int meshSeq;
    private String pmid;
    private String meshTerms;
    private String meshUi;
    private String majorTopicYn;
}
