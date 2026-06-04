package com.omnibooking.specification;

import com.omnibooking.dto.search.SearchCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

@RequiredArgsConstructor
public class GenericSpecification<T> implements Specification<T> {

   private final SearchCriteria criteria;

   @Override
   public Predicate toPredicate(@NonNull Root<T> root, @Nullable CriteriaQuery<?> query,
         @NonNull CriteriaBuilder builder) {
      if (criteria.getOperation().equalsIgnoreCase(">")) {
         return builder.greaterThanOrEqualTo(
               root.get(criteria.getKey()), criteria.getValue().toString());
      } else if (criteria.getOperation().equalsIgnoreCase("<")) {
         return builder.lessThanOrEqualTo(
               root.get(criteria.getKey()), criteria.getValue().toString());
      } else if (criteria.getOperation().equalsIgnoreCase(":")) {
         if (root.get(criteria.getKey()).getJavaType() == String.class) {
            return builder.like(
                  root.get(criteria.getKey()), "%" + criteria.getValue() + "%");
         } else {
            return builder.equal(root.get(criteria.getKey()), criteria.getValue());
         }
      }

      return null;
   }

}
