package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.Trade;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeSpecificationsUnitTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void forCounterpartyName_buildsCaseInsensitiveLikePredicate() {
        Root root = mock(Root.class);
        Path counterpartyPath = mock(Path.class);
        Path namePath = mock(Path.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Expression<String> lowerName = mock(Expression.class);
        Predicate predicate = mock(Predicate.class);

        when(root.get("counterparty")).thenReturn(counterpartyPath);
        when(counterpartyPath.get("name")).thenReturn(namePath);
        when(cb.lower(namePath)).thenReturn(lowerName);
        when(cb.like(lowerName, "%apex%")).thenReturn(predicate);

        Predicate result = TradeSpecifications.forCounterpartyName("Apex")
                .toPredicate(root, null, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).lower(namePath);
        verify(cb).like(lowerName, "%apex%");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void forCounterpartyName_blankValue_returnsConjunction() {
        Root root = mock(Root.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);

        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = TradeSpecifications.forCounterpartyName(" ")
                .toPredicate(root, null, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).conjunction();
        verify(root, never()).get(anyString());
    }
}
