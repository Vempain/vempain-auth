package fi.poltsi.vempain.auth.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Embeddable
public class UserUnitId implements Serializable {

    @Column(name = "user_id")
    private Long userId;
    @Column(name = "unit_id")
    private Long unitId;
}
