-- Align reactions.reaction_type with the JPA mapping (Reaction.reactionType is a String).
-- V8 created this column as ENUM('like','dislike'), which Hibernate schema-validation
-- rejects under the mysql-dev/prod profiles (expects VARCHAR), preventing app startup.
-- Reaction type values are already validated in ReactionService, so a plain VARCHAR is sufficient.
ALTER TABLE reactions MODIFY reaction_type VARCHAR(20) NOT NULL;
