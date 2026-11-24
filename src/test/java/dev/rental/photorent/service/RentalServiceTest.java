package dev.rental.photorent.service;

import dev.rental.photorent.FixedClockConfig;
import dev.rental.photorent.TestData;
import dev.rental.photorent.concurrent.RentalOverdueProcessor;
import dev.rental.photorent.model.CustomerUser;
import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentStatus;
import dev.rental.photorent.model.Rental;
import dev.rental.photorent.model.RentalStatus;
import dev.rental.photorent.repository.EquipmentRepository;
import dev.rental.photorent.repository.RentalRepository;
import dev.rental.photorent.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {RentalService.class, RentalOverdueProcessor.class, FixedClockConfig.class})
class RentalServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 12, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 5);

    @Autowired
    private RentalService rentalService;

    @MockitoBean
    private RentalRepository rentalRepository;
    @MockitoBean
    private EquipmentRepository equipmentRepository;
    @MockitoBean
    private UserRepository userRepository;

    private final Equipment camera =
            TestData.equipment(1L, EquipmentStatus.AVAILABLE, EquipmentCondition.GOOD);
    private final CustomerUser customer = TestData.customer(3L);

    @BeforeEach
    void setUp() {
        when(equipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(camera));
        when(userRepository.findCustomerById(3L)).thenReturn(Optional.of(customer));
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // бронирование

    @Test
    void shouldBookRentalWhenDatesAreFree() {
        Rental rental = rentalService.bookRental(1L, 3L, START, END);

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.BOOKED);
        assertThat(rental.getEquipment()).isEqualTo(camera);
        assertThat(rental.getCustomer()).isEqualTo(customer);
    }

    // проверка бага из лр1 (техника у другого клиента сейчас, но на будущие даты её можно бронировать)
    @Test
    void shouldBookEquipmentThatIsRentedRightNowForFutureDates() {
        camera.setStatus(EquipmentStatus.RENTED);

        Rental rental = rentalService.bookRental(1L, 3L, START, END);

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.BOOKED);
    }

    @Test
    void shouldCheckConflictsAgainstAllOpenStatuses() {
        rentalService.bookRental(1L, 3L, START, END);

        verify(rentalRepository).findOverlapping(1L, RentalStatus.OPEN_STATUSES, START, END);
        assertThat(RentalStatus.OPEN_STATUSES)
                .containsExactlyInAnyOrder(RentalStatus.BOOKED, RentalStatus.ACTIVE, RentalStatus.OVERDUE);
    }

    @Test
    void shouldRejectStartDateAfterEndDate() {
        assertThatThrownBy(() -> rentalService.bookRental(1L, 3L, END, START))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMissingDates() {
        assertThatThrownBy(() -> rentalService.bookRental(1L, 3L, null, END))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectEquipmentThatNeedsRepair() {
        camera.setCondition(EquipmentCondition.NEEDS_REPAIR);

        assertThatThrownBy(() -> rentalService.bookRental(1L, 3L, START, END))
                .isInstanceOf(IllegalStateException.class);
        verify(rentalRepository, never()).save(any(Rental.class));
    }

    @Test
    void shouldRejectOverlappingBooking() {
        Rental existing = TestData.rental(10L, camera, TestData.customer(4L), START, END, RentalStatus.BOOKED);
        when(rentalRepository.findOverlapping(1L, RentalStatus.OPEN_STATUSES, START, END))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> rentalService.bookRental(1L, 3L, START, END))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already booked");
        verify(rentalRepository, never()).save(any(Rental.class));
    }

    @Test
    void shouldRejectUnknownEquipment() {
        assertThatThrownBy(() -> rentalService.bookRental(99L, 3L, START, END))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void shouldRejectUnknownCustomer() {
        assertThatThrownBy(() -> rentalService.bookRental(1L, 99L, START, END))
                .isInstanceOf(NoSuchElementException.class);
    }

    // выдача и возврат

    @Test
    void shouldIssueBookedRentalAndUpdateStatuses() {
        Rental rental = storedRental(RentalStatus.BOOKED);

        rentalService.issueEquipment(10L);

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(camera.getStatus()).isEqualTo(EquipmentStatus.RENTED);
    }

    @Test
    void shouldRejectIssuingRentalThatIsNotBooked() {
        storedRental(RentalStatus.ACTIVE);

        assertThatThrownBy(() -> rentalService.issueEquipment(10L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectIssuingWhenEquipmentIsNotAvailableRightNow() {
        Rental rental = storedRental(RentalStatus.BOOKED);
        camera.setStatus(EquipmentStatus.RENTED);

        assertThatThrownBy(() -> rentalService.issueEquipment(10L))
                .isInstanceOf(IllegalStateException.class);
        assertThat(rental.getStatus()).isEqualTo(RentalStatus.BOOKED);
    }

    @Test
    void shouldReturnActiveRentalAndFreeEquipment() {
        Rental rental = storedRental(RentalStatus.ACTIVE);
        camera.setStatus(EquipmentStatus.RENTED);

        rentalService.returnEquipment(10L);

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.RETURNED);
        assertThat(camera.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    void shouldReturnOverdueRental() {
        Rental rental = storedRental(RentalStatus.OVERDUE);
        camera.setStatus(EquipmentStatus.RENTED);

        rentalService.returnEquipment(10L);

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.RETURNED);
    }

    @Test
    void shouldRejectReturningBookedRental() {
        storedRental(RentalStatus.BOOKED);

        assertThatThrownBy(() -> rentalService.returnEquipment(10L))
                .isInstanceOf(IllegalStateException.class);
    }

    // продление

    @Test
    void shouldExtendActiveRentalWhenNoConflicts() {
        Rental rental = storedRental(RentalStatus.ACTIVE);
        LocalDate newEnd = END.plusDays(3);

        rentalService.extendRental(10L, newEnd);

        assertThat(rental.getEndDate()).isEqualTo(newEnd);
        verify(rentalRepository).findOverlappingExcluding(
                1L, 10L, RentalStatus.OPEN_STATUSES, END.plusDays(1), newEnd);
    }

    @Test
    void shouldRejectExtensionWhenAnotherCustomerBookedNextPeriod() {
        Rental rental = storedRental(RentalStatus.ACTIVE);
        LocalDate newEnd = END.plusDays(3);
        Rental next = TestData.rental(11L, camera, TestData.customer(4L),
                END.plusDays(1), END.plusDays(5), RentalStatus.BOOKED);
        when(rentalRepository.findOverlappingExcluding(
                1L, 10L, RentalStatus.OPEN_STATUSES, END.plusDays(1), newEnd))
                .thenReturn(List.of(next));

        assertThatThrownBy(() -> rentalService.extendRental(10L, newEnd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(next.getStartDate().toString());
        assertThat(rental.getEndDate()).isEqualTo(END);
    }

    @Test
    void shouldRejectExtensionToEarlierDate() {
        storedRental(RentalStatus.ACTIVE);

        assertThatThrownBy(() -> rentalService.extendRental(10L, END.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectExtensionOfNotActiveRental() {
        storedRental(RentalStatus.BOOKED);

        assertThatThrownBy(() -> rentalService.extendRental(10L, END.plusDays(3)))
                .isInstanceOf(IllegalStateException.class);
    }

    // просрочки

    @Test
    void shouldMarkOnlyPastDueActiveRentalsAsOverdue() {
        Rental pastDue = TestData.rental(1L, camera, customer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), RentalStatus.ACTIVE);
        Rental notDue = TestData.rental(2L, camera, customer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31), RentalStatus.ACTIVE);
        when(rentalRepository.findByStatus(RentalStatus.ACTIVE)).thenReturn(List.of(pastDue, notDue));

        int marked = rentalService.markOverdueRentals(LocalDate.of(2026, 11, 18));

        assertThat(marked).isEqualTo(1);
        assertThat(pastDue.getStatus()).isEqualTo(RentalStatus.OVERDUE);
        assertThat(notDue.getStatus()).isEqualTo(RentalStatus.ACTIVE);
    }

    // ---- даты брони ----

    @Test
    void shouldRejectStartDateInThePast() {
        LocalDate yesterday = FixedClockConfig.TODAY.minusDays(1);

        assertThatThrownBy(() -> rentalService.bookRental(1L, 3L, yesterday, END))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past");
        verify(rentalRepository, never()).save(any(Rental.class));
    }

    @Test
    void shouldAllowBookingThatStartsToday() {
        Rental rental = rentalService.bookRental(1L, 3L, FixedClockConfig.TODAY, END);

        assertThat(rental.getStartDate()).isEqualTo(FixedClockConfig.TODAY);
    }

    // ---- отмена брони ----

    @Test
    void shouldCancelBookedRental() {
        Rental rental = storedRental(RentalStatus.BOOKED);

        rentalService.cancelBooking(10L);

        verify(rentalRepository).delete(rental);
    }

    @Test
    void shouldRejectCancellingActiveRentalAndKeepIt() {
        storedRental(RentalStatus.ACTIVE);

        assertThatThrownBy(() -> rentalService.cancelBooking(10L))
                .isInstanceOf(IllegalStateException.class);
        verify(rentalRepository, never()).delete(any(Rental.class));
    }

    @Test
    void shouldRejectCancellingUnknownRental() {
        assertThatThrownBy(() -> rentalService.cancelBooking(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ---- просрочка без указанной даты ----

    @Test
    void shouldUseTodayFromClockWhenDateIsNotGiven() {
        Rental pastDue = TestData.rental(1L, camera, customer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 10), RentalStatus.ACTIVE);
        Rental notDue = TestData.rental(2L, camera, customer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 18), RentalStatus.ACTIVE);
        when(rentalRepository.findByStatus(RentalStatus.ACTIVE)).thenReturn(List.of(pastDue, notDue));

        int marked = rentalService.markOverdueRentals(null);

        assertThat(marked).isEqualTo(1);
        assertThat(pastDue.getStatus()).isEqualTo(RentalStatus.OVERDUE);
        assertThat(notDue.getStatus()).isEqualTo(RentalStatus.ACTIVE);
    }

    private Rental storedRental(RentalStatus status) {
        Rental rental = TestData.rental(10L, camera, customer, START, END, status);
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental));
        return rental;
    }
}
