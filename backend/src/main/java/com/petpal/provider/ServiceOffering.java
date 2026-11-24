package com.petpal.provider;

import com.petpal.catalog.ServiceCategory;
import com.petpal.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Duration;

@Entity
@Table(name = "service_offerings")
public class ServiceOffering extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false, updatable = false)
    private ProviderProfile provider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private boolean active;

    protected ServiceOffering() {
    }

    public static ServiceOffering create(ProviderProfile provider, ServiceCategory category, String title,
            String description, BigDecimal price, int durationMinutes) {
        ServiceOffering offering = new ServiceOffering();
        offering.provider = provider;
        offering.active = true;
        offering.update(category, title, description, price, durationMinutes);
        return offering;
    }

    public void update(ServiceCategory category, String title, String description, BigDecimal price,
            int durationMinutes) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.price = price;
        this.durationMinutes = durationMinutes;
    }

    public void deactivate() {
        this.active = false;
    }

    public Duration duration() {
        return Duration.ofMinutes(durationMinutes);
    }

    public ProviderProfile getProvider() {
        return provider;
    }

    public ServiceCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public boolean isActive() {
        return active;
    }
}
