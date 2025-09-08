package dev.aparikh.searchdifftool;

import org.springframework.boot.SpringApplication;

public class TestSearchDiffToolApplication {

    public static void main(String[] args) {
        SpringApplication.from(SearchDiffToolApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }

}
