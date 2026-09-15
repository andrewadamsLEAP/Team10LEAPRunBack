package com.example.repositories;

import com.example.dbclasses.Prices;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceRepository extends JpaRepository<Prices, Long> {

}
