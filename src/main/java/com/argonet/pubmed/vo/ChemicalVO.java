package com.argonet.pubmed.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChemicalVO {
    private int articleId;
    private int chemicalSeq;
    private String pmid;
    private String registryNumber;
    private String chemicalSubstance;
    private String chemicalUi;
}
