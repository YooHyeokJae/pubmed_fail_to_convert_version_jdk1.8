package com.argonet.pubmed.mapper;

import com.argonet.pubmed.vo.*;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PubmedMapper {
    int getArticleId(String pmid);

    int getSavedVersion(String pmid);

    int insertPubmedArticle(PubmedArticleVO pubmedArticle);

    int deleteAffiliation(String pmid);

    int deleteAuthor(String pmid);

    int deleteMeshHeading(String pmid);

    int deleteChemical(String pmid);

    int deleteGrant(String pmid);

    int deleteReference(String pmid);

    int updatePubmedArticle(PubmedArticleVO pubmedArticle);

    int unionInsertAuthor(List<AuthorVO> authorVOList);

    int unionInsertAffiliation(List<AffiliationVO> affiliationVOList);

    int unionInsertMeshHeading(List<MeshHeadingVO> meshHeadingVOList);

    int unionInsertChemical(List<ChemicalVO> chemicalVOList);

    int unionInsertGrant(List<GrantVO> grantVOList);

    int unionInsertReference(List<ReferenceVO> referenceVOList);
}
